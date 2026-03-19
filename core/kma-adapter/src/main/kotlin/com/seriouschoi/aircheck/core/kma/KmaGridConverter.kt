package com.seriouschoi.aircheck.core.kma

import kotlin.math.*

/**
 * 위경도 ↔ 기상청 격자 좌표 변환기
 * 
 * Lambert Conformal Conic Projection 사용
 * 기상청 격자: 149 x 253 (5km 간격)
 */
object KmaGridConverter {
    
    // 기상청 격자 파라미터
    private const val RE = 6371.00877     // 지구 반경 (km)
    private const val GRID = 5.0          // 격자 간격 (km)
    private const val SLAT1 = 30.0        // 표준 위도 1
    private const val SLAT2 = 60.0        // 표준 위도 2
    private const val OLON = 126.0        // 기준점 경도
    private const val OLAT = 38.0         // 기준점 위도
    private const val XO = 43.0           // 기준점 X좌표
    private const val YO = 136.0          // 기준점 Y좌표
    
    data class Grid(val nx: Int, val ny: Int)
    
    /**
     * 위경도 → 격자 좌표 변환
     */
    fun toGrid(lat: Double, lng: Double): Grid {
        val degRad = PI / 180.0
        val re = RE / GRID
        val slat1 = SLAT1 * degRad
        val slat2 = SLAT2 * degRad
        val olon = OLON * degRad
        val olat = OLAT * degRad
        
        var sn = tan(PI * 0.25 + slat2 * 0.5) / tan(PI * 0.25 + slat1 * 0.5)
        sn = ln(cos(slat1) / cos(slat2)) / ln(sn)
        
        var sf = tan(PI * 0.25 + slat1 * 0.5)
        sf = sf.pow(sn) * cos(slat1) / sn
        
        var ro = tan(PI * 0.25 + olat * 0.5)
        ro = re * sf / ro.pow(sn)
        
        var ra = tan(PI * 0.25 + lat * degRad * 0.5)
        ra = re * sf / ra.pow(sn)
        
        var theta = lng * degRad - olon
        if (theta > PI) theta -= 2.0 * PI
        if (theta < -PI) theta += 2.0 * PI
        theta *= sn
        
        val nx = (ra * sin(theta) + XO + 0.5).toInt()
        val ny = (ro - ra * cos(theta) + YO + 0.5).toInt()
        
        return Grid(nx, ny)
    }
    
    /**
     * 격자 좌표 → 위경도 변환
     */
    fun toLatLng(nx: Int, ny: Int): Pair<Double, Double> {
        val degRad = PI / 180.0
        val re = RE / GRID
        val slat1 = SLAT1 * degRad
        val slat2 = SLAT2 * degRad
        val olon = OLON * degRad
        val olat = OLAT * degRad
        
        var sn = tan(PI * 0.25 + slat2 * 0.5) / tan(PI * 0.25 + slat1 * 0.5)
        sn = ln(cos(slat1) / cos(slat2)) / ln(sn)
        
        var sf = tan(PI * 0.25 + slat1 * 0.5)
        sf = sf.pow(sn) * cos(slat1) / sn
        
        var ro = tan(PI * 0.25 + olat * 0.5)
        ro = re * sf / ro.pow(sn)
        
        val xn = nx - XO
        val yn = ro - ny + YO
        
        var ra = sqrt(xn * xn + yn * yn)
        if (sn < 0) ra = -ra
        
        var alat = (re * sf / ra).pow(1.0 / sn)
        alat = 2.0 * atan(alat) - PI * 0.5
        
        var theta = if (abs(xn) <= 0.0) 0.0 else {
            if (abs(yn) <= 0.0) {
                if (xn < 0) -PI * 0.5 else PI * 0.5
            } else {
                atan2(xn, yn)
            }
        }
        val alon = theta / sn + olon
        
        return Pair(alat / degRad, alon / degRad)
    }
}
