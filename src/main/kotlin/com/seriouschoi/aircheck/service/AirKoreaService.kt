package com.seriouschoi.aircheck.service

import com.seriouschoi.aircheck.model.AirGrade
import com.seriouschoi.aircheck.model.AirQualityResponse
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

// ── 측정소 정보 (좌표 포함) ─────────────────────────────────────────────────

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
    val dmX: String = "",  // 위도
    val dmY: String = ""   // 경도
)

// ── Nominatim 응답 ─────────────────────────────────────────────────────────

@Serializable
data class NominatimResponse(val address: NominatimAddress? = null)

@Serializable
data class NominatimAddress(
    val state: String = "",
    val city: String? = null,
    val city_district: String? = null,
    val suburb: String? = null,
    val town: String? = null,
    val village: String? = null
)

// ── 서비스 ─────────────────────────────────────────────────────────────────

@Service
class AirKoreaService(
    @Value("\${airkorea.api-key}") private val apiKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }
    
    // 측정소 좌표 캐시 (앱 시작 시 로드)
    private var stationCoordinates: Map<String, Pair<Double, Double>> = emptyMap()
    
    /**
     * 측정소 목록 로드 (좌표 포함)
     */
    fun loadStationCoordinates() {
        try {
            val url = "https://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getMsrstnList" +
                    "?pageNo=1&numOfRows=700&returnType=json" +
                    "&serviceKey=${URLEncoder.encode(apiKey, "UTF-8")}" +
                    "&ver=1.1"
            
            val response = get(url)
            val parsed = json.decodeFromString<StationResponse>(response)
            
            stationCoordinates = parsed.response?.body?.items?.mapNotNull { station ->
                val lat = station.dmX.toDoubleOrNull()
                val lng = station.dmY.toDoubleOrNull()
                if (lat != null && lng != null) {
                    station.stationName to Pair(lat, lng)
                } else null
            }?.toMap() ?: emptyMap()
            
            log.info("측정소 ${stationCoordinates.size}개 로드 완료")
        } catch (e: Exception) {
            log.error("측정소 로드 실패: ${e.message}")
        }
    }
    
    /**
     * 좌표 기반 가장 가까운 측정소 찾기
     */
    fun findNearestStation(lat: Double, lng: Double): String? {
        if (stationCoordinates.isEmpty()) loadStationCoordinates()
        
        return stationCoordinates.minByOrNull { (_, coords) ->
            val (sLat, sLng) = coords
            Math.sqrt(Math.pow(lat - sLat, 2.0) + Math.pow(lng - sLng, 2.0))
        }?.key
    }
    
    /**
     * 좌표로 시도명 얻기 (Nominatim)
     */
    @Cacheable("sido")
    fun getSidoName(lat: Double, lng: Double): String? {
        val url = "https://nominatim.openstreetmap.org/reverse" +
                "?lat=$lat&lon=$lng&format=json&accept-language=ko&addressdetails=1"
        
        return try {
            val response = get(url)
            val parsed = json.decodeFromString<NominatimResponse>(response)
            parsed.address?.state
        } catch (e: Exception) {
            log.error("Nominatim 호출 실패: ${e.message}")
            null
        }
    }
    
    /**
     * 대기질 정보 조회 (좌표 기반)
     */
    @Cacheable("airquality", key = "#lat + ',' + #lng")
    fun getAirQuality(lat: Double, lng: Double): AirQualityResponse? {
        // 1. 가장 가까운 측정소 찾기
        val stationName = findNearestStation(lat, lng) ?: return null
        
        // 2. 측정소 데이터 조회
        val url = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty" +
                "?stationName=${URLEncoder.encode(stationName, "UTF-8")}" +
                "&dataTerm=DAILY&pageNo=1&numOfRows=1&returnType=json" +
                "&serviceKey=${URLEncoder.encode(apiKey, "UTF-8")}" +
                "&ver=1.0"
        
        return try {
            val response = get(url)
            val parsed = json.decodeFromString<AirKoreaResponse>(response)
            val item = parsed.response?.body?.items?.firstOrNull() ?: return null
            
            val pm10 = item.pm10Value.toIntOrNull()
            val pm25 = item.pm25Value.toIntOrNull()
            val aqi = item.khaiValue.toIntOrNull()
            
            val pm10Grade = AirGrade.fromPm10(pm10)
            val pm25Grade = AirGrade.fromPm25(pm25)
            val aqiGrade = AirGrade.fromAqi(aqi)
            
            AirQualityResponse(
                stationName = item.stationName,
                sidoName = item.sidoName,
                dataTime = item.dataTime,
                pm10 = pm10,
                pm25 = pm25,
                aqi = aqi,
                pm10Grade = pm10Grade,
                pm25Grade = pm25Grade,
                aqiGrade = aqiGrade,
                worstGrade = AirGrade.worst(pm10Grade, pm25Grade, aqiGrade)
            )
        } catch (e: Exception) {
            log.error("대기질 조회 실패: ${e.message}")
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
