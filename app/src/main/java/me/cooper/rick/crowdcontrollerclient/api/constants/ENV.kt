package me.cooper.rick.crowdcontrollerclient.api.constants

val environment: Environment = Dev()

val BASE_REST_URL = environment.restUrl
val BASE_WS_URL = environment.websocketUrl

sealed class Environment {

    val restUrl: String = "$restProtocol://$host:$port"
    val websocketUrl: String = "$websocketProtocol://$host:$port"

    protected abstract val host: String
    protected abstract val restProtocol: String
    protected abstract val websocketProtocol: String
    protected abstract val port: String
}

class Dev : Environment() {

    override val host: String
        get() = "rickcooper.ddns.net"
    override val restProtocol: String
        get() = "http"
    override val websocketProtocol: String
        get() = "ws"
    override val port: String
        get() = "8080"

}

class Prod : Environment() {

    override val host: String
        get() = "crowdcontroller.ddns.net"
    override val restProtocol: String
        get() = "https"
    override val websocketProtocol: String
        get() = "wss"
    override val port: String
        get() = "8443"

}
