package me.cooper.rick.crowdcontrollerclient.api.util

import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto
import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto.Companion.DEFAULT_DESCRIPTION
import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto.Companion.DEFAULT_ERROR
import me.cooper.rick.crowdcontrollerclient.api.constants.HttpStatus.Companion.BAD_REQUEST
import me.cooper.rick.crowdcontrollerclient.api.constants.HttpStatus.Companion.SERVICE_UNAVAILABLE
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import okhttp3.MediaType.parse
import okhttp3.ResponseBody
import okhttp3.ResponseBody.create
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Response.error
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

const val BAD_PASSWORD = "Password Incorrect"
const val BAD_USERNAME = "Username Incorrect"

private const val BAD_USERNAME_INCOMING_ERROR = "unauthorized"
private const val BAD_PASSWORD_INCOMING_ERROR = "invalid_grant"
private const val BAD_USERNAME_DESCRIPTION = "The username entered does not exist. Please try again."
private const val BAD_PASSWORD_DESCRIPTION = "The password entered is incorrect. Please try again."
private const val CONNECTION_ERROR_BODY = "{\"error\":\"$DEFAULT_ERROR\", \"error_description\":\"$DEFAULT_DESCRIPTION\"}"

fun <Any> parseError(response: Response<Any>): APIErrorDto {
    val converter: Converter<ResponseBody, APIErrorDto> = ServiceGenerator
            .retrofit()
            .responseBodyConverter(APIErrorDto::class.java, emptyArray())
    return try {
        val conversion = converter.convert(response.errorBody()!!)
        when (conversion.error) {
            BAD_PASSWORD_INCOMING_ERROR -> APIErrorDto(BAD_REQUEST, BAD_PASSWORD, BAD_PASSWORD_DESCRIPTION)
            BAD_USERNAME_INCOMING_ERROR -> APIErrorDto(BAD_REQUEST, BAD_USERNAME, BAD_USERNAME_DESCRIPTION)
            else -> conversion
        }
    } catch (e: IOException) {
        APIErrorDto()
    }
}

fun <T : Any> buildConnectionExceptionResponse(e: IOException): Response<T> {
    return when (e) {
        is ConnectException, is SocketTimeoutException -> {
            error<T>(SERVICE_UNAVAILABLE,
                    create(parse("application/json"), CONNECTION_ERROR_BODY))
        }
        else -> throw e
    }
}
