package me.cooper.rick.crowdcontrollerclient.api.model

/*
https://futurestud.io/tutorials/retrofit-2-simple-error-handling
 */
data class APIError(val statusCode: Int = 0, val message: String? = null)