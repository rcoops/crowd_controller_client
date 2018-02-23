package me.cooper.rick.crowdcontrollerclient.api.util

import me.cooper.rick.crowdcontrollerclient.api.model.APIError
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Response
import java.io.IOException

class ErrorUtils private constructor() {

    companion object {

        fun parseError(response: Response<Any>): APIError {
            val converter: Converter<ResponseBody, APIError> = ServiceGenerator
                    .retrofit()
                    .responseBodyConverter(APIError::class.java, emptyArray())
            return try {
                converter.convert(response.errorBody()!!)
            } catch (e: IOException) {
                APIError()
            }
        }

    }

}