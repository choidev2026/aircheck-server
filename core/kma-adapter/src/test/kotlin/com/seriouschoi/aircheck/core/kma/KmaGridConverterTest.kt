package com.seriouschoi.aircheck.core.kma

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 기상청 격자 변환 테스트
 */
class KmaGridConverterTest {
    
    @Test
    fun `서울 강남역 좌표 변환`() {
        // Given: 강남역 위경도
        val lat = 37.4979
        val lng = 127.0276
        
        // When
        val grid = KmaGridConverter.toGrid(lat, lng)
        
        // Then: 기상청 격자 (약 61, 126 예상)
        println("강남역: ($lat, $lng) → Grid(${grid.nx}, ${grid.ny})")
        assertTrue(grid.nx in 55..65)
        assertTrue(grid.ny in 120..130)
    }
    
    @Test
    fun `서울시청 좌표 변환`() {
        // Given: 서울시청
        val lat = 37.5666
        val lng = 126.9784
        
        // When
        val grid = KmaGridConverter.toGrid(lat, lng)
        
        // Then: 기상청 격자 (약 60, 127 예상)
        println("서울시청: ($lat, $lng) → Grid(${grid.nx}, ${grid.ny})")
        assertTrue(grid.nx in 55..65)
        assertTrue(grid.ny in 125..130)
    }
    
    @Test
    fun `부산 해운대 좌표 변환`() {
        // Given: 해운대
        val lat = 35.1628
        val lng = 129.1658
        
        // When
        val grid = KmaGridConverter.toGrid(lat, lng)
        
        // Then
        println("해운대: ($lat, $lng) → Grid(${grid.nx}, ${grid.ny})")
        assertTrue(grid.nx in 95..105)
        assertTrue(grid.ny in 75..85)
    }
    
    @Test
    fun `제주 좌표 변환`() {
        // Given: 제주시
        val lat = 33.4996
        val lng = 126.5312
        
        // When
        val grid = KmaGridConverter.toGrid(lat, lng)
        
        // Then
        println("제주: ($lat, $lng) → Grid(${grid.nx}, ${grid.ny})")
        assertTrue(grid.nx in 50..60)
        assertTrue(grid.ny in 30..40)
    }
    
    @Test
    fun `역변환 테스트`() {
        // Given: 서울 격자
        val nx = 60
        val ny = 127
        
        // When
        val (lat, lng) = KmaGridConverter.toLatLng(nx, ny)
        
        // Then: 서울 부근 좌표
        println("Grid($nx, $ny) → ($lat, $lng)")
        assertTrue(lat in 37.0..38.0)
        assertTrue(lng in 126.5..127.5)
    }
    
    @Test
    fun `변환-역변환 일관성`() {
        // Given
        val originalLat = 37.5
        val originalLng = 127.0
        
        // When: 변환 후 역변환
        val grid = KmaGridConverter.toGrid(originalLat, originalLng)
        val (restoredLat, restoredLng) = KmaGridConverter.toLatLng(grid.nx, grid.ny)
        
        // Then: 오차 0.1도 이내
        println("원본: ($originalLat, $originalLng)")
        println("격자: (${grid.nx}, ${grid.ny})")
        println("복원: ($restoredLat, $restoredLng)")
        
        assertEquals(originalLat, restoredLat, 0.1)
        assertEquals(originalLng, restoredLng, 0.1)
    }
}
