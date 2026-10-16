package com.seriouschoi.aircheck.core.kma

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange

interface KmaApi {

    @GetExchange("/getUltraSrtNcst")
    fun getUltraSrtNcst(
        @RequestParam("serviceKey") serviceKey: String,
        @RequestParam("pageNo") pageNo: Int,
        @RequestParam("numOfRows") numOfRows: Int,
        @RequestParam("dataType") dataType: String,
        @RequestParam("base_date") baseDate: String,
        @RequestParam("base_time") baseTime: String,
        @RequestParam("nx") nx: Int,
        @RequestParam("ny") ny: Int
    ): String

    @GetExchange("/getVilageFcst")
    fun getVilageFcst(
        @RequestParam("serviceKey") serviceKey: String,
        @RequestParam("pageNo") pageNo: Int,
        @RequestParam("numOfRows") numOfRows: Int,
        @RequestParam("dataType") dataType: String,
        @RequestParam("base_date") baseDate: String,
        @RequestParam("base_time") baseTime: String,
        @RequestParam("nx") nx: Int,
        @RequestParam("ny") ny: Int
    ): String
}

interface KmaMidApi {

    @GetExchange("/getMidLandFcst")
    fun getMidLandFcst(
        @RequestParam("serviceKey") serviceKey: String,
        @RequestParam("pageNo") pageNo: Int,
        @RequestParam("numOfRows") numOfRows: Int,
        @RequestParam("dataType") dataType: String,
        @RequestParam("regId") regId: String,
        @RequestParam("tmFc") tmFc: String
    ): String

    @GetExchange("/getMidTa")
    fun getMidTa(
        @RequestParam("serviceKey") serviceKey: String,
        @RequestParam("pageNo") pageNo: Int,
        @RequestParam("numOfRows") numOfRows: Int,
        @RequestParam("dataType") dataType: String,
        @RequestParam("regId") regId: String,
        @RequestParam("tmFc") tmFc: String
    ): String
}
