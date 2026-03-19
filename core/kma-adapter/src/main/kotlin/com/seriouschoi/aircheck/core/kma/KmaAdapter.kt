package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.CurrentWeather
import com.seriouschoi.aircheck.core.domain.model.HourlyForecast
import com.seriouschoi.aircheck.core.domain.model.WeatherCondition
import com.seriouschoi.aircheck.core.domain.model.WeatherResponse
import com.seriouschoi.aircheck.core.domain.port.WeatherPort
import com.seriouschoi.aircheck.core.kma.dto.FcstItem
import com.seriouschoi.aircheck.core.kma.dto.KmaApiResponse
import com.seriouschoi.aircheck.core.kma.dto.NcstItem
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

/**
 * 기상청 API Adapter
 * 
 * - 초단기실황: 현재 날씨
 * - 초단기예보: 6시간 예보
 * - 단기예보: 3일 예보
 * 
 * 활성화: weather.provider=kma
 */
@Component
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "kma")
class KmaAdapter(
    private val restTemplate: RestTemplate,
    @Value("\${kma.api.key:}") private val apiKey: String,
    @Value("\${kma.api.base-url:http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0}") 
    private val baseUrl: String
) : WeatherPort {
    
    companion object {
        private const val ULTRA_SRT_NCST = "getUltraSrtNcst"  // 초단기실황
        private const val ULTRA_SRT_FCST = "getUltraSrtFcst"  // 초단기예보
        private const val VILAGE_FCST = "getVilageFcst"       // 단기예보
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }
    
    @Cacheable("kma-weather", key = "#lat + '_' + #lng")
    override fun getWeather(lat: Double, lng: Double): WeatherResponse? {
        if (apiKey.isBlank()) {
            logger.warn { "KMA API key not configured" }
            return null
        }
        
        return try {
            val grid = KmaGridConverter.toGrid(lat, lng)
            logger.debug { "Converted ($lat, $lng) to grid (${grid.nx}, ${grid.ny})" }
            
            // 현재 시간 기준 base_date, base_time 계산
            val (baseDate, baseTime) = calculateBaseDateTime()
            
            // 초단기실황 조회 (현재 날씨)
            val currentWeather = fetchCurrentWeather(grid.nx, grid.ny, baseDate, baseTime)
            
            // 초단기예보 조회 (6시간 예보)
            val hourlyForecast = fetchHourlyForecast(grid.nx, grid.ny, baseDate, baseTime)
            
            if (currentWeather == null) {
                logger.warn { "Failed to fetch current weather" }
                return null
            }
            
            WeatherResponse(
                current = currentWeather,
                hourlyForecast = hourlyForecast
            )
        } catch (e: Exception) {
            logger.error(e) { "KMA API error: ${e.message}" }
            null
        }
    }
    
    /**
     * 초단기실황 조회
     */
    private fun fetchCurrentWeather(nx: Int, ny: Int, baseDate: String, baseTime: String): CurrentWeather? {
        val url = buildUrl(ULTRA_SRT_NCST, nx, ny, baseDate, baseTime)
        
        return try {
            val response = restTemplate.getForObject(url, String::class.java) ?: return null
            val parsed = json.decodeFromString<KmaApiResponse<NcstItem>>(response)
            
            if (parsed.response.header.resultCode != "00") {
                logger.warn { "KMA API error: ${parsed.response.header.resultMsg}" }
                return null
            }
            
            val items = parsed.response.body?.items?.item ?: return null
            parseCurrentWeather(items)
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch current weather: ${e.message}" }
            null
        }
    }
    
    /**
     * 초단기예보 조회
     */
    private fun fetchHourlyForecast(nx: Int, ny: Int, baseDate: String, baseTime: String): List<HourlyForecast> {
        val url = buildUrl(ULTRA_SRT_FCST, nx, ny, baseDate, baseTime)
        
        return try {
            val response = restTemplate.getForObject(url, String::class.java) ?: return emptyList()
            val parsed = json.decodeFromString<KmaApiResponse<FcstItem>>(response)
            
            if (parsed.response.header.resultCode != "00") {
                logger.warn { "KMA API error: ${parsed.response.header.resultMsg}" }
                return emptyList()
            }
            
            val items = parsed.response.body?.items?.item ?: return emptyList()
            parseHourlyForecast(items)
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch hourly forecast: ${e.message}" }
            emptyList()
        }
    }
    
    /**
     * API URL 생성
     */
    private fun buildUrl(operation: String, nx: Int, ny: Int, baseDate: String, baseTime: String): String {
        return "$baseUrl/$operation" +
                "?serviceKey=$apiKey" +
                "&pageNo=1" +
                "&numOfRows=60" +
                "&dataType=JSON" +
                "&base_date=$baseDate" +
                "&base_time=$baseTime" +
                "&nx=$nx" +
                "&ny=$ny"
    }
    
    /**
     * base_date, base_time 계산
     * 
     * 초단기실황: 정시 기준, 40분 후 생성
     * 초단기예보: 30분 기준
     */
    private fun calculateBaseDateTime(): Pair<String, String> {
        val now = LocalDateTime.now()
        
        // 초단기실황은 매시 정각 발표, 40분 후 생성
        // 안전하게 1시간 전 데이터 사용
        val baseTime = if (now.minute < 40) {
            now.minusHours(1)
        } else {
            now
        }
        
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val timeFormatter = DateTimeFormatter.ofPattern("HH00")
        
        return Pair(
            baseTime.format(dateFormatter),
            baseTime.format(timeFormatter)
        )
    }
    
    /**
     * 초단기실황 파싱
     */
    private fun parseCurrentWeather(items: List<NcstItem>): CurrentWeather {
        var temperature = 0.0
        var humidity = 0
        var precipitation = 0.0
        var windSpeed = 0.0
        var pty = 0  // 강수형태
        
        items.forEach { item ->
            when (item.category) {
                "T1H" -> temperature = item.obsrValue.toDoubleOrNull() ?: 0.0
                "REH" -> humidity = item.obsrValue.toIntOrNull() ?: 0
                "RN1" -> precipitation = item.obsrValue.toDoubleOrNull() ?: 0.0
                "WSD" -> windSpeed = item.obsrValue.toDoubleOrNull() ?: 0.0
                "PTY" -> pty = item.obsrValue.toIntOrNull() ?: 0
            }
        }
        
        // 체감온도 계산 (간단한 공식)
        val feelsLike = calculateFeelsLike(temperature, windSpeed, humidity)
        
        // 현재 시간 기준 낮/밤 판단
        val hour = LocalDateTime.now().hour
        val isDay = hour in 6..18
        
        // 날씨 상태 변환
        val weatherCondition = convertToWeatherCondition(pty = pty, sky = 1) // 실황은 SKY 없음
        
        return CurrentWeather(
            temperature = temperature,
            feelsLike = feelsLike,
            precipitation = precipitation,
            weatherCode = pty,
            weatherCondition = weatherCondition,
            cloudCover = 0, // 실황에서는 제공 안 함
            isDay = isDay
        )
    }
    
    /**
     * 초단기예보 파싱
     */
    private fun parseHourlyForecast(items: List<FcstItem>): List<HourlyForecast> {
        // 시간별로 그룹핑
        val groupedByTime = items.groupBy { "${it.fcstDate}_${it.fcstTime}" }
        
        return groupedByTime.map { (timeKey, timeItems) ->
            val fcstDate = timeKey.split("_")[0]
            val fcstTime = timeKey.split("_")[1]
            val hour = fcstTime.substring(0, 2).toInt()
            
            var temperature = 0.0
            var sky = 1
            var pty = 0
            var pop = 0
            var sno = 0.0
            
            timeItems.forEach { item ->
                when (item.category) {
                    "T1H" -> temperature = item.fcstValue.toDoubleOrNull() ?: 0.0
                    "SKY" -> sky = item.fcstValue.toIntOrNull() ?: 1
                    "PTY" -> pty = item.fcstValue.toIntOrNull() ?: 0
                    "POP" -> pop = item.fcstValue.toIntOrNull() ?: 0
                    "SNO" -> sno = item.fcstValue.toDoubleOrNull() ?: 0.0
                }
            }
            
            val weatherCondition = convertToWeatherCondition(pty, sky)
            
            HourlyForecast(
                time = "${fcstDate}T${fcstTime.substring(0, 2)}:00",
                hour = hour,
                temperature = temperature,
                feelsLike = temperature, // 예보에서는 체감온도 별도 계산 필요
                precipitationProbability = pop,
                snowfall = sno,
                weatherCode = pty * 10 + sky,
                weatherCondition = weatherCondition
            )
        }.sortedBy { it.time }.take(6)
    }
    
    /**
     * 체감온도 계산
     */
    private fun calculateFeelsLike(temp: Double, windSpeed: Double, humidity: Int): Double {
        return when {
            // 추운 날씨: Wind Chill
            temp <= 10 && windSpeed >= 1.3 -> {
                val ws016 = Math.pow(windSpeed, 0.16)
                13.12 + 0.6215 * temp - 11.37 * ws016 + 0.3965 * temp * ws016
            }
            // 더운 날씨: Heat Index (간단 버전)
            temp >= 27 -> {
                temp + 0.33 * (humidity / 100.0 * 6.105 * Math.exp(17.27 * temp / (237.7 + temp))) - 4.0
            }
            else -> temp
        }
    }
    
    /**
     * 기상청 코드 → WeatherCondition 변환
     * 
     * PTY (강수형태): 0없음, 1비, 2비/눈, 3눈, 5빗방울, 6빗방울눈날림, 7눈날림
     * SKY (하늘상태): 1맑음, 3구름많음, 4흐림
     */
    private fun convertToWeatherCondition(pty: Int, sky: Int): WeatherCondition {
        // 강수가 있으면 강수 우선
        return when (pty) {
            1, 5 -> WeatherCondition.RAIN
            2, 6 -> WeatherCondition.RAIN  // 비/눈 섞임
            3, 7 -> WeatherCondition.SNOW
            else -> {
                // 강수 없으면 하늘 상태로 판단
                when (sky) {
                    1 -> WeatherCondition.CLEAR
                    3 -> WeatherCondition.PARTLY_CLOUDY
                    4 -> WeatherCondition.CLOUDY
                    else -> WeatherCondition.CLEAR
                }
            }
        }
    }
}
