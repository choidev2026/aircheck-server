package com.seriouschoi.aircheck.adapter.out.api

import com.seriouschoi.aircheck.domain.model.AirGrade
import com.seriouschoi.aircheck.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.domain.port.out.AirQualityPort
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
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
    
    // 측정소 좌표 캐시
    private var stationCoordinates: Map<String, Pair<Double, Double>> = emptyMap()

    override fun loadStationCoordinates() {
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

    private fun findNearestStation(lat: Double, lng: Double): String? {
        if (stationCoordinates.isEmpty()) loadStationCoordinates()
        
        return stationCoordinates.minByOrNull { (_, coords) ->
            val (sLat, sLng) = coords
            Math.sqrt(Math.pow(lat - sLat, 2.0) + Math.pow(lng - sLng, 2.0))
        }?.key
    }

    override fun getAirQuality(lat: Double, lng: Double): AirQualityResponse? {
        val stationName = findNearestStation(lat, lng) ?: return null
        
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
            log.error("에어코리아 API 호출 실패: ${e.message}")
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
