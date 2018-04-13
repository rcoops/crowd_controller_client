package me.cooper.rick.crowdcontrollerclient.api.constants
private const val PROD_HOST = "crowdcontroller.ddns.net"
private const val PROD_PROTOCOL = "https"
private const val PROD_PORT = "8443"
private const val DEV_HOST = "rickcooper.ddns.net"
private const val DEV_PROTOCOL = "http"
private const val DEV_PORT = "8080"
private const val PROD_REST_URL = "$PROD_PROTOCOL://$PROD_HOST:$PROD_PORT"
private const val DEV_REST_URL = "$DEV_PROTOCOL://$DEV_HOST:$DEV_PORT"
private const val PROD_WS_URL = "wss://$PROD_HOST:$PROD_PORT"
private const val DEV_WS_URL = "ws://$DEV_HOST:$DEV_PORT"

private const val isProd = false

val BASE_REST_URL = if (isProd) PROD_REST_URL else DEV_REST_URL
val BASE_WS_URL = if (isProd) PROD_WS_URL else DEV_WS_URL

