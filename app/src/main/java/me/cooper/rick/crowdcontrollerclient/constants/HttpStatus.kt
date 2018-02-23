package me.cooper.rick.crowdcontrollerclient.constants

/**
 * Created by rick on 23/02/18.
 */
class HttpStatus private constructor() {

    companion object {
        const val OK: Int = 200
        const val BAD_REQUEST: Int = 400
        const val UNAUTHORIZED: Int = 401
        const val NOT_FOUND: Int = 404
        const val SERVICE_UNAVAILABLE: Int = 503
    }

}