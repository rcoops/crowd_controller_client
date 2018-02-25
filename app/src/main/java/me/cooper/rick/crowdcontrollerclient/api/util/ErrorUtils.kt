package me.cooper.rick.crowdcontrollerclient.api.util

import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto
import me.cooper.rick.crowdcontrollerclient.constants.HttpStatus
import me.cooper.rick.crowdcontrollerclient.constants.HttpStatus.Companion.BAD_REQUEST
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

fun <Any> parseError(response: Response<Any>): APIErrorDto {
    val converter: Converter<ResponseBody, APIErrorDto> = ServiceGenerator
            .retrofit()
            .responseBodyConverter(APIErrorDto::class.java, emptyArray())
    return try {
        val conversion = converter.convert(response.errorBody()!!)
        return when (conversion.error) {
            "invalid_grant", "unauthorized" -> APIErrorDto(BAD_REQUEST, "Bad Credentials", "Your username or password is incorrect. Please try again.")
            else -> conversion
        }
    } catch (e: IOException) {
        APIErrorDto()
    }
}

fun <Any> handleConnectionException(e: IOException): Response<Any> {
    return when (e) {
        is ConnectException, is SocketTimeoutException -> {
            Response.error<Any>(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    ResponseBody.create(MediaType.parse("application/json"), "")
            )
        }
        else -> throw e
    }
}
