package me.cooper.rick.crowdcontrollerclient.api.util

import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Response
import java.io.IOException

fun parseError(response: Response<Any>): APIErrorDto {
    val converter: Converter<ResponseBody, APIErrorDto> = ServiceGenerator
            .retrofit()
            .responseBodyConverter(APIErrorDto::class.java, emptyArray())
    return try {
        converter.convert(response.errorBody()!!)
    } catch (e: IOException) {
        APIErrorDto()
    }
}