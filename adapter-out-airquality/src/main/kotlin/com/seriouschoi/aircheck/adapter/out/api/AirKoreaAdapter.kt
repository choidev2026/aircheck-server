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
    val pm10Value: String = "-",
    val pm25Value: String = "-",
    val khaiValue: String = "-"
)

// ── 측정소 정보 ────────────────────────────────────────────────────────────

@Serializable
data class StationResponse(val response: StationBody? = null)

@Serializable
data class StationBody(val body: StationItems? = null)

@Serializable
data class StationItems(val items: List<StationItem> = emptyList())

@Serializable
data class StationItem(
    val stationName: String = "",
    val addr: String = "",
    val dmX: String = "",
    val dmY: String = ""
)

// ── 측정소 캐시 데이터 ──────────────────────────────────────────────────────

private data class StationInfo(
    val lat: Double,
    val lng: Double,
    val sidoName: String
)

// ── Adapter 구현 ───────────────────────────────────────────────────────────

@Component
class AirKoreaAdapter(
    @Value("\${airkorea.api-key}") private val apiKey: String
) : AirQualityPort {
    
    private val log = LoggerFactory.getLogger(javaClass)
    
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
    
    // 측정소 정보 캐시 (이름 → 좌표 + 시도명)
    private var stationCache: Map<String, StationInfo> = emptyMap()

    override fun loadStationCoordinates() {
        try {
            val url = "https://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getMsrstnList" +
                    "?pageNo=1&numOfRows=700&returnType=json" +
                    "&serviceKey=${URLEncoder.encode(apiKey, "UTF-8")}" +
                    "&ver=1.1"
            
            val response = get(url)
            val parsed = json.decodeFromString<StationResponse>(response)
            
            stationCache = parsed.response?.body?.items?.mapNotNull { station ->
                // dmX = 경도(longitude), dmY = 위도(latitude)
                val lng = station.dmX.toDoubleOrNull()
                val lat = station.dmY.toDoubleOrNull()
                if (lat != null && lng != null) {
                    // 주소에서 시도명 추출 (예: "서울 중구 덕수궁길 15" → "서울")
                    val sidoName = station.addr.split(" ").firstOrNull() ?: ""
                    station.stationName to StationInfo(lat, lng, sidoName)
                } else null
            }?.toMap() ?: emptyMap()
            
            log.info("측정소 ${stationCache.size}개 로드 완료")
        } catch (e: Exception) {
            log.error("측정소 로드 실패: ${e.message}")
        }
    }

    /**
     * 가까운 측정소 N개를 거리순으로 반환
     */
    private fun findNearestStations(lat: Double, lng: Double, limit: Int = 5): List<Pair<String, StationInfo>> {
        if (stationCache.isEmpty()) loadStationCoordinates()
        
        return stationCache.entries
            .sortedBy { (_, info) ->
                Math.sqrt(Math.pow(lat - info.lat, 2.0) + Math.pow(lng - info.lng, 2.0))
            }
            .take(limit)
            .map { it.key to it.value }
    }

    override fun getAirQuality(lat: Double, lng: Double): AirQualityResponse? {
        val nearbyStations = findNearestStations(lat, lng, limit = 5)
        if (nearbyStations.isEmpty()) return null
        
        // 가까운 측정소부터 순서대로 시도
        for ((stationName, stationInfo) in nearbyStations) {
            val result = fetchFromStation(stationName, stationInfo)
            if (result != null && (result.pm10 != null || result.pm25 != null)) {
                return result
            }
            log.debug("측정소 {} 데이터 없음, 다음 측정소 시도", stationName)
        }
        
        log.warn("주변 {}개 측정소 모두 데이터 없음 (lat={}, lng={})", nearbyStations.size, lat, lng)
        return null
    }
    
    /**
     * 특정 측정소에서 대기질 정보 조회
     */
    private fun fetchFromStation(stationName: String, stationInfo: StationInfo): AirQualityResponse? {
        val url = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty" +
                "?stationName=${URLEncoder.encode(stationName, "UTF-8")}" +
                "&dataTerm=DAILY&pageNo=1&numOfRows=1&returnType=json" +
                "&serviceKey=${URLEncoder.encode(apiKey, "UTF-8")}" +
                "&ver=1.0"
        
        return try {
            val response = get(url)
            val parsed = json.decodeFromString<AirKoreaResponse>(response)
            val item = parsed.response?.body?.items?.firstOrNull() ?: return null
            
            AirQualityResponse(
                stationName = stationName,
                sidoName = stationInfo.sidoName,
                dataTime = item.dataTime,
                pm10 = item.pm10Value.toIntOrNull(),
                pm25 = item.pm25Value.toIntOrNull()
            )
        } catch (e: Exception) {
            log.error("측정소 {} 조회 실패: {}", stationName, e.message)
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
