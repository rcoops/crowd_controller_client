package me.cooper.rick.crowdcontrollerclient.api.util

import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto
import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto.Companion.DEFAULT_DESCRIPTION
import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto.Companion.DEFAULT_ERROR
import me.cooper.rick.crowdcontrollerclient.constants.HttpStatus.Companion.SERVICE_UNAVAILABLE
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

private const val BAD_USERNAME_INCOMING_ERROR = "unauthorized"
private const val BAD_PASSWORD_INCOMING_ERROR = "invalid_grant"
private const val OUTGOING_ERROR = "Bad Credentials"
private const val BAD_USERNAME_DESCRIPTION = "The username entered is incorrect. Please try again."
private const val BAD_PASSWORD_DESCRIPTION = "The password entered is incorrect. Please try again."
private const val CONNECTION_ERROR_BODY = "{\"error\":\"$DEFAULT_ERROR\", \"error_description\":\"$DEFAULT_DESCRIPTION\"}"

fun <Any> parseError(response: Response<Any>): APIErrorDto {
    val converter: Converter<ResponseBody, APIErrorDto> = ServiceGenerator
            .retrofit()
            .responseBodyConverter(APIErrorDto::class.java, emptyArray())

    val conversion = converter.convert(response.errorBody()!!)

    return when (conversion.error) {
        BAD_USERNAME_INCOMING_ERROR -> APIErrorDto(conversion.status, OUTGOING_ERROR, BAD_USERNAME_DESCRIPTION)
        BAD_PASSWORD_INCOMING_ERROR -> APIErrorDto(conversion.status, OUTGOING_ERROR, BAD_PASSWORD_DESCRIPTION)
        else -> conversion
    }
}

fun <Any> handleConnectionException(e: IOException): Response<Any> {
    return when (e) {
        is ConnectException, is SocketTimeoutException -> {
            Response.error<Any>(
                    SERVICE_UNAVAILABLE,
                    ResponseBody.create(
                            MediaType.parse("application/json"), CONNECTION_ERROR_BODY
                    )
            )
        }
        else -> throw e
    }
}
