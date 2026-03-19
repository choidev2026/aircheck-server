package com.seriouschoi.aircheck.adapter.out.api

import com.seriouschoi.aircheck.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.domain.port.out.AirQualityPort
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

// ── 에어코리아 API 응답 모델 ───────────────────────────────────────────────

@Serializable
data class AirKoreaResponse(val response: AirKoreaBody? = null)

@Serializable
data class AirKoreaBody(val body: AirKoreaItems? = null)

@Serializable
data class AirKoreaItems(val items: List<AirKoreaItem> = emptyList())

@Serializable
data class AirKoreaItem(
    val stationName: String = "",
    val sidoName: String = "",
    val dataTime: String = "",
    // 미세먼지
    val pm10Value: String = "-",
    val pm10Grade: String = "-",
    val pm25Value: String = "-",
    val pm25Grade: String = "-",
    // 통합지수
    val khaiValue: String = "-",
    val khaiGrade: String = "-",
    // 가스류
    val so2Value: String = "-",    // 아황산가스 (ppm)
    val so2Grade: String = "-",
    val coValue: String = "-",     // 일산화탄소 (ppm)
    val coGrade: String = "-",
    val o3Value: String = "-",     // 오존 (ppm)
    val o3Grade: String = "-",
    val no2Value: String = "-",    // 이산화질소 (ppm)
    val no2Grade: String = "-"
)

// ── Adapter 구현 ───────────────────────────────────────────────────────────

@Component
class AirKoreaAdapter(
    @Value("\${airkorea.api-key}") private val apiKey: String,
    private val apiUsagePort: com.seriouschoi.aircheck.application.port.out.ApiUsagePort?,
    private val stationCacheService: StationCacheService
) : AirQualityPort {
    
    private val log = LoggerFactory.getLogger(javaClass)
    
    companion object {
        private const val API_TYPE = "AIR_KOREA"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .addHeader("User-Agent", "AirCheckServer/1.0")
                .build())
        }
        .build()
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    /**
     * 가까운 측정소 N개를 거리순으로 반환
     */
    private fun findNearestStations(lat: Double, lng: Double, limit: Int = 5): List<Pair<String, StationInfo>> {
        val stations = stationCacheService.loadStations()
        
        return stations.entries
            .sortedBy { (_, info) ->
                Math.sqrt(Math.pow(lat - info.lat, 2.0) + Math.pow(lng - info.lng, 2.0))
            }
            .take(limit)
            .map { it.key to it.value }
    }

    override fun getAirQuality(lat: Double, lng: Double): AirQualityResponse? {
        val nearbyStations = findNearestStations(lat, lng, limit = 5)
        if (nearbyStations.isEmpty()) return null
        
        // 메인 측정소 (가장 가까운 곳)
        val (mainStation, mainInfo) = nearbyStations.first()
        val mainData = fetchRawData(mainStation)
        
        // 결과 변수 (기본값: 메인 측정소)
        var pm10: Int? = mainData?.pm10Value?.toIntOrNull()
        var pm10Station: String = mainStation
        var pm25: Int? = mainData?.pm25Value?.toIntOrNull()
        var pm25Station: String = mainStation
        var dataTime = mainData?.dataTime ?: ""
        
        // PM10이 없으면 주변 측정소에서 채우기
        if (pm10 == null) {
            for ((station, _) in nearbyStations.drop(1)) {
                val data = fetchRawData(station)
                val value = data?.pm10Value?.toIntOrNull()
                if (value != null) {
                    pm10 = value
                    pm10Station = station
                    if (dataTime.isEmpty()) dataTime = data.dataTime
                    log.debug("PM10: {} 측정소에서 가져옴 (값: {})", station, value)
                    break
                }
            }
        }
        
        // PM25가 없으면 주변 측정소에서 채우기
        if (pm25 == null) {
            for ((station, _) in nearbyStations.drop(1)) {
                val data = fetchRawData(station)
                val value = data?.pm25Value?.toIntOrNull()
                if (value != null) {
                    pm25 = value
                    pm25Station = station
                    if (dataTime.isEmpty()) dataTime = data.dataTime
                    log.debug("PM25: {} 측정소에서 가져옴 (값: {})", station, value)
                    break
                }
            }
        }
        
        // 둘 다 없으면 실패
        if (pm10 == null && pm25 == null) {
            log.warn("주변 {}개 측정소 모두 PM 데이터 없음 (lat={}, lng={})", nearbyStations.size, lat, lng)
            return null
        }
        
        return AirQualityResponse(
            stationName = mainStation,
            sidoName = mainInfo.sidoName,
            dataTime = dataTime,
            pm10 = pm10,
            pm10Station = pm10Station,
            pm25 = pm25,
            pm25Station = pm25Station,
            // 통합지수 (메인 측정소)
            khaiValue = mainData?.khaiValue?.toIntOrNull(),
            khaiGrade = mainData?.khaiGrade?.toIntOrNull(),
            // 가스류 (메인 측정소)
            so2 = mainData?.so2Value?.toDoubleOrNull(),
            co = mainData?.coValue?.toDoubleOrNull(),
            o3 = mainData?.o3Value?.toDoubleOrNull(),
            no2 = mainData?.no2Value?.toDoubleOrNull()
        )
    }
    
    /**
     * 측정소에서 원시 데이터 조회 (AirKoreaItem 반환)
     */
    private fun fetchRawData(stationName: String): AirKoreaItem? {
        val url = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty" +
                "?stationName=${URLEncoder.encode(stationName, "UTF-8")}" +
                "&dataTerm=DAILY&pageNo=1&numOfRows=1&returnType=json" +
                "&serviceKey=${URLEncoder.encode(apiKey, "UTF-8")}" +
                "&ver=1.0"
        
        val startTime = System.currentTimeMillis()
        
        return try {
            val response = get(url)
            val parsed = json.decodeFromString<AirKoreaResponse>(response)
            
            val responseTime = System.currentTimeMillis() - startTime
            apiUsagePort?.recordSuccess(API_TYPE, responseTime)
            
            parsed.response?.body?.items?.firstOrNull()
        } catch (e: Exception) {
            log.error("측정소 {} 조회 실패: {}", stationName, e.message)
            apiUsagePort?.recordFailure(API_TYPE, e.message)
            null
        }
    }
    
    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "AirCheckServer/1.0")
            .build()
        return client.newCall(request).execute().use { it.body?.string() ?: "" }
    }
}
