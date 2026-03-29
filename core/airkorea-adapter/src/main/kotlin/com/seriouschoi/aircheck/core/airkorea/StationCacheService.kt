package com.seriouschoi.aircheck.core.airkorea

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 측정소 정보 캐시 서비스
 * 
 * 측정소 목록은 거의 변하지 않으므로 장기 캐싱
 */
@Service
class StationCacheService(
    @Value("\${airkorea.api-key}") private val apiKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 전국 측정소 정보 로드 (캐시 적용)
     * 
     * @return 측정소명 → StationInfo 맵
     */
    @Cacheable("stations", unless = "#result.isEmpty()")
    fun loadStations(): Map<String, StationInfo> {
        log.info("측정소 정보 로드 시작 (API 호출)")
        
        return try {
            val url = "https://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getMsrstnList" +
                    "?pageNo=1&numOfRows=700&returnType=json" +
                    "&serviceKey=${URLEncoder.encode(apiKey, "UTF-8")}" +
                    "&ver=1.1"
            
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute().use { it.body?.string() ?: "" }
            val parsed = json.decodeFromString<StationResponse>(response)
            
            val stations = parsed.response?.body?.items?.mapNotNull { station ->
                val lng = station.dmX.toDoubleOrNull()
                val lat = station.dmY.toDoubleOrNull()
                if (lat != null && lng != null) {
                    val sidoName = station.addr.split(" ").firstOrNull() ?: ""
                    station.stationName to StationInfo(lat, lng, sidoName)
                } else null
            }?.toMap() ?: emptyMap()
            
            log.info("측정소 ${stations.size}개 로드 완료")
            stations
        } catch (e: Exception) {
            log.error("측정소 로드 실패: ${e.message}")
            emptyMap()
        }
    }
}

// ── 모델 ───────────────────────────────────────────────────────────────────

data class StationInfo(
    val lat: Double,
    val lng: Double,
    val sidoName: String
)

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
