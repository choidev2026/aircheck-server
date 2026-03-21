package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.CurrentWeather
import com.seriouschoi.aircheck.core.domain.model.DailyForecast
import com.seriouschoi.aircheck.core.domain.model.HourlyForecast
import com.seriouschoi.aircheck.core.domain.model.MidTermForecast
import com.seriouschoi.aircheck.core.domain.model.WeatherCondition
import com.seriouschoi.aircheck.core.domain.model.WeatherResponse
import com.seriouschoi.aircheck.core.domain.port.WeatherPort
import com.seriouschoi.aircheck.core.kma.dto.FcstItem
import com.seriouschoi.aircheck.core.kma.dto.KmaApiResponse
import com.seriouschoi.aircheck.core.kma.dto.MidLandFcstItem
import com.seriouschoi.aircheck.core.kma.dto.MidTaItem
import com.seriouschoi.aircheck.core.kma.dto.NcstItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
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
    private val baseUrl: String,
    @Value("\${kma.api.mid-url:http://apis.data.go.kr/1360000/MidFcstInfoService}") 
    private val midBaseUrl: String
) : WeatherPort {
    
    companion object {
        private const val ULTRA_SRT_NCST = "getUltraSrtNcst"  // 초단기실황
        private const val ULTRA_SRT_FCST = "getUltraSrtFcst"  // 초단기예보
        private const val VILAGE_FCST = "getVilageFcst"       // 단기예보
        private const val MID_LAND_FCST = "getMidLandFcst"    // 중기육상예보
        private const val MID_TA = "getMidTa"                 // 중기기온예보
        
        // 중기예보 지역코드 (서울/경기 기준, 추후 확장)
        private const val DEFAULT_REG_ID = "11B00000"  // 서울/인천/경기
        private const val DEFAULT_TA_REG_ID = "11B10101"  // 서울
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
            val (vilageFcstDate, vilageFcstTime) = calculateVilageFcstBaseDateTime()
            
            // 병렬 API 호출
            runBlocking(Dispatchers.IO) {
                val currentDeferred = async { fetchCurrentWeather(grid.nx, grid.ny, baseDate, baseTime) }
                val ultraShortDeferred = async { fetchHourlyForecast(grid.nx, grid.ny, baseDate, baseTime) }
                val vilageFcstDeferred = async { fetchVilageFcstItems(grid.nx, grid.ny, vilageFcstDate, vilageFcstTime) }
                val midTermDeferred = async { fetchMidTermForecast() }
                
                val currentWeather = currentDeferred.await()
                val ultraShortForecast = ultraShortDeferred.await()
                val vilageFcstItems = vilageFcstDeferred.await()
                val midTermForecast = midTermDeferred.await()
                
                // 단기예보 파싱
                val dailyForecast = parseDailyForecast(vilageFcstItems)
                val extendedHourlyForecast = parseExtendedHourlyForecast(vilageFcstItems)
                val hourlyForecast = combineHourlyForecasts(ultraShortForecast, extendedHourlyForecast)
                
                if (currentWeather == null) {
                    logger.warn { "Failed to fetch current weather" }
                    return@runBlocking null
                }
                
                WeatherResponse(
                    current = currentWeather,
                    hourlyForecast = hourlyForecast,
                    dailyForecast = dailyForecast,
                    midTermForecast = midTermForecast
                )
            }
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
     * 단기예보 원본 데이터 조회 (3일)
     */
    private fun fetchVilageFcstItems(nx: Int, ny: Int, baseDate: String, baseTime: String): List<FcstItem> {
        val url = buildUrl(VILAGE_FCST, nx, ny, baseDate, baseTime, numOfRows = 1000)
        
        return try {
            val response = restTemplate.getForObject(url, String::class.java) ?: return emptyList()
            val parsed = json.decodeFromString<KmaApiResponse<FcstItem>>(response)
            
            if (parsed.response.header.resultCode != "00") {
                logger.warn { "KMA API error: ${parsed.response.header.resultMsg}" }
                return emptyList()
            }
            
            parsed.response.body?.items?.item ?: emptyList()
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch vilage forecast: ${e.message}" }
            emptyList()
        }
    }
    
    /**
     * 중기예보 조회 (3~10일)
     */
    private fun fetchMidTermForecast(): List<MidTermForecast> {
        val tmFc = calculateMidFcstTmFc()
        
        return try {
            // 중기육상예보 (날씨)
            val landUrl = "$midBaseUrl/$MID_LAND_FCST" +
                    "?serviceKey=$apiKey" +
                    "&pageNo=1&numOfRows=10&dataType=JSON" +
                    "&regId=$DEFAULT_REG_ID" +
                    "&tmFc=$tmFc"
            
            // 중기기온예보 (기온)
            val taUrl = "$midBaseUrl/$MID_TA" +
                    "?serviceKey=$apiKey" +
                    "&pageNo=1&numOfRows=10&dataType=JSON" +
                    "&regId=$DEFAULT_TA_REG_ID" +
                    "&tmFc=$tmFc"
            
            val landResponse = restTemplate.getForObject(landUrl, String::class.java)
            val taResponse = restTemplate.getForObject(taUrl, String::class.java)
            
            if (landResponse == null || taResponse == null) return emptyList()
            
            val landParsed = json.decodeFromString<KmaApiResponse<MidLandFcstItem>>(landResponse)
            val taParsed = json.decodeFromString<KmaApiResponse<MidTaItem>>(taResponse)
            
            if (landParsed.response.header.resultCode != "00" || 
                taParsed.response.header.resultCode != "00") {
                return emptyList()
            }
            
            val landItem = landParsed.response.body?.items?.item?.firstOrNull()
            val taItem = taParsed.response.body?.items?.item?.firstOrNull()
            
            if (landItem == null || taItem == null) return emptyList()
            
            parseMidTermForecast(landItem, taItem)
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch mid-term forecast: ${e.message}" }
            emptyList()
        }
    }
    
    /**
     * API URL 생성
     */
    private fun buildUrl(
        operation: String, 
        nx: Int, 
        ny: Int, 
        baseDate: String, 
        baseTime: String,
        numOfRows: Int = 60
    ): String {
        return "$baseUrl/$operation" +
                "?serviceKey=$apiKey" +
                "&pageNo=1" +
                "&numOfRows=$numOfRows" +
                "&dataType=JSON" +
                "&base_date=$baseDate" +
                "&base_time=$baseTime" +
                "&nx=$nx" +
                "&ny=$ny"
    }
    
    /**
     * base_date, base_time 계산 (초단기)
     * 
     * 초단기실황: 정시 기준, 40분 후 생성
     * 초단기예보: 30분 기준
     */
    private fun calculateBaseDateTime(): Pair<String, String> {
        val koreaZone = java.time.ZoneId.of("Asia/Seoul")
        val now = java.time.ZonedDateTime.now(koreaZone).toLocalDateTime()
        
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
     * 단기예보 base_date, base_time 계산
     * 
     * 발표시각: 02, 05, 08, 11, 14, 17, 20, 23시
     */
    private fun calculateVilageFcstBaseDateTime(): Pair<String, String> {
        val koreaZone = java.time.ZoneId.of("Asia/Seoul")
        val now = java.time.ZonedDateTime.now(koreaZone).toLocalDateTime()
        val hour = now.hour
        
        // 발표 시각 목록 (10분 후 사용 가능)
        val baseTimes = listOf(2, 5, 8, 11, 14, 17, 20, 23)
        
        // 현재 시간 기준 가장 최근 발표 시각 찾기
        val baseHour = baseTimes.lastOrNull { it <= hour - 1 } ?: 23
        
        val baseDateTime = if (baseHour == 23 && hour < 23) {
            now.minusDays(1).withHour(23)
        } else {
            now.withHour(baseHour)
        }
        
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        
        return Pair(
            baseDateTime.format(dateFormatter),
            String.format("%02d00", baseHour)
        )
    }
    
    /**
     * 중기예보 발표시각 (tmFc) 계산
     * 
     * 발표시각: 06시, 18시
     */
    private fun calculateMidFcstTmFc(): String {
        val now = LocalDateTime.now()
        val hour = now.hour
        
        val baseDateTime = when {
            hour < 6 -> now.minusDays(1).withHour(18)
            hour < 18 -> now.withHour(6)
            else -> now.withHour(18)
        }
        
        return baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMddHH00"))
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
            
            val dateTime = LocalDateTime.parse(
                "${fcstDate}${fcstTime}",
                DateTimeFormatter.ofPattern("yyyyMMddHHmm")
            )
            
            HourlyForecast(
                time = dateTime,
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
     * 단기예보에서 48시간 시간별 예보 파싱
     */
    private fun parseExtendedHourlyForecast(items: List<FcstItem>): List<HourlyForecast> {
        if (items.isEmpty()) return emptyList()
        
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
                    "TMP" -> temperature = item.fcstValue.toDoubleOrNull() ?: 0.0
                    "SKY" -> sky = item.fcstValue.toIntOrNull() ?: 1
                    "PTY" -> pty = item.fcstValue.toIntOrNull() ?: 0
                    "POP" -> pop = item.fcstValue.toIntOrNull() ?: 0
                    "SNO" -> sno = item.fcstValue.toDoubleOrNull() ?: 0.0
                }
            }
            
            val weatherCondition = convertToWeatherCondition(pty, sky)
            
            val dateTime = LocalDateTime.parse(
                "${fcstDate}${fcstTime}",
                DateTimeFormatter.ofPattern("yyyyMMddHHmm")
            )
            
            HourlyForecast(
                time = dateTime,
                hour = hour,
                temperature = temperature,
                feelsLike = temperature,
                precipitationProbability = pop,
                snowfall = sno,
                weatherCode = pty * 10 + sky,
                weatherCondition = weatherCondition
            )
        }.sortedBy { it.time }.take(48)
    }
    
    /**
     * 초단기예보와 단기예보 시간별 데이터 결합
     * - 초단기예보: 6시간 (더 정확)
     * - 단기예보: 7~48시간
     * - 현재 시간 이후만 포함
     */
    private fun combineHourlyForecasts(
        ultraShort: List<HourlyForecast>,
        extended: List<HourlyForecast>
    ): List<HourlyForecast> {
        // KMA 데이터는 KST 기준이므로 KST로 비교
        val koreaZone = java.time.ZoneId.of("Asia/Seoul")
        val nowKst = java.time.ZonedDateTime.now(koreaZone).toLocalDateTime()
        val currentHour = nowKst.withMinute(0).withSecond(0).withNano(0)
        
        // 현재 시간 이후만 필터링
        val filteredUltraShort = ultraShort.filter { it.time >= currentHour }
        val filteredExtended = extended.filter { it.time >= currentHour }
        
        if (filteredUltraShort.isEmpty()) return filteredExtended.take(48)
        if (filteredExtended.isEmpty()) return filteredUltraShort.take(48)
        
        // 초단기예보의 시간들
        val ultraShortTimes = filteredUltraShort.map { it.time }.toSet()
        
        // 중복되지 않는 단기예보만 추가
        val additionalHours = filteredExtended.filter { it.time !in ultraShortTimes }
        
        return (filteredUltraShort + additionalHours).sortedBy { it.time }.take(48)
    }
    
    /**
     * 단기예보 파싱 (일별)
     */
    private fun parseDailyForecast(items: List<FcstItem>): List<DailyForecast> {
        // 날짜별로 그룹핑
        val groupedByDate = items.groupBy { it.fcstDate }
        val dayOfWeekFormatter = DateTimeFormatter.ofPattern("E", java.util.Locale.KOREAN)
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        
        return groupedByDate.map { (date, dateItems) ->
            var minTemp = Double.MAX_VALUE
            var maxTemp = Double.MIN_VALUE
            var amPop = 0
            var pmPop = 0
            var amSky = 1
            var pmSky = 1
            var amPty = 0
            var pmPty = 0
            
            dateItems.forEach { item ->
                val hour = item.fcstTime.substring(0, 2).toIntOrNull() ?: 0
                val isAm = hour < 12
                
                when (item.category) {
                    "TMN" -> minTemp = item.fcstValue.toDoubleOrNull() ?: minTemp
                    "TMX" -> maxTemp = item.fcstValue.toDoubleOrNull() ?: maxTemp
                    "TMP" -> {
                        val temp = item.fcstValue.toDoubleOrNull() ?: 0.0
                        if (temp < minTemp) minTemp = temp
                        if (temp > maxTemp) maxTemp = temp
                    }
                    "POP" -> {
                        val pop = item.fcstValue.toIntOrNull() ?: 0
                        if (isAm) amPop = maxOf(amPop, pop)
                        else pmPop = maxOf(pmPop, pop)
                    }
                    "SKY" -> {
                        val sky = item.fcstValue.toIntOrNull() ?: 1
                        if (isAm) amSky = sky
                        else pmSky = sky
                    }
                    "PTY" -> {
                        val pty = item.fcstValue.toIntOrNull() ?: 0
                        if (isAm) amPty = pty
                        else pmPty = pty
                    }
                }
            }
            
            val localDate = java.time.LocalDate.parse(date, dateFormatter)
            
            DailyForecast(
                date = "${localDate.year}-${String.format("%02d", localDate.monthValue)}-${String.format("%02d", localDate.dayOfMonth)}",
                dayOfWeek = localDate.format(dayOfWeekFormatter),
                minTemp = if (minTemp == Double.MAX_VALUE) 0.0 else minTemp,
                maxTemp = if (maxTemp == Double.MIN_VALUE) 0.0 else maxTemp,
                amPrecipProb = amPop,
                pmPrecipProb = pmPop,
                amWeatherCondition = convertToWeatherCondition(amPty, amSky),
                pmWeatherCondition = convertToWeatherCondition(pmPty, pmSky)
            )
        }.sortedBy { it.date }.take(3)
    }
    
    /**
     * 중기예보 파싱
     */
    private fun parseMidTermForecast(landItem: MidLandFcstItem, taItem: MidTaItem): List<MidTermForecast> {
        val today = java.time.LocalDate.now()
        val dayOfWeekFormatter = DateTimeFormatter.ofPattern("E", java.util.Locale.KOREAN)
        
        return (3..10).mapNotNull { dayOffset ->
            val date = today.plusDays(dayOffset.toLong())
            
            val (amPop, pmPop, wfAm, wfPm) = when (dayOffset) {
                3 -> listOf(landItem.rnSt3Am, landItem.rnSt3Pm, landItem.wf3Am, landItem.wf3Pm)
                4 -> listOf(landItem.rnSt4Am, landItem.rnSt4Pm, landItem.wf4Am, landItem.wf4Pm)
                5 -> listOf(landItem.rnSt5Am, landItem.rnSt5Pm, landItem.wf5Am, landItem.wf5Pm)
                6 -> listOf(landItem.rnSt6Am, landItem.rnSt6Pm, landItem.wf6Am, landItem.wf6Pm)
                7 -> listOf(landItem.rnSt7Am, landItem.rnSt7Pm, landItem.wf7Am, landItem.wf7Pm)
                8 -> listOf(landItem.rnSt8, landItem.rnSt8, landItem.wf8, landItem.wf8)
                9 -> listOf(landItem.rnSt9, landItem.rnSt9, landItem.wf9, landItem.wf9)
                10 -> listOf(landItem.rnSt10, landItem.rnSt10, landItem.wf10, landItem.wf10)
                else -> return@mapNotNull null
            }
            
            val (minTemp, maxTemp) = when (dayOffset) {
                3 -> Pair(taItem.taMin3, taItem.taMax3)
                4 -> Pair(taItem.taMin4, taItem.taMax4)
                5 -> Pair(taItem.taMin5, taItem.taMax5)
                6 -> Pair(taItem.taMin6, taItem.taMax6)
                7 -> Pair(taItem.taMin7, taItem.taMax7)
                8 -> Pair(taItem.taMin8, taItem.taMax8)
                9 -> Pair(taItem.taMin9, taItem.taMax9)
                10 -> Pair(taItem.taMin10, taItem.taMax10)
                else -> return@mapNotNull null
            }
            
            MidTermForecast(
                date = "${date.year}-${String.format("%02d", date.monthValue)}-${String.format("%02d", date.dayOfMonth)}",
                dayOfWeek = date.format(dayOfWeekFormatter),
                minTemp = minTemp ?: 0,
                maxTemp = maxTemp ?: 0,
                amPrecipProb = (amPop as? Int) ?: 0,
                pmPrecipProb = (pmPop as? Int) ?: 0,
                weatherDescription = (wfAm as? String) ?: "맑음"
            )
        }
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
