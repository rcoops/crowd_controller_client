package me.cooper.rick.crowdcontrollerclient.api.constants

val environment: Environment = Prod()

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

    override val host: String = "rickcooper.ddns.net"
    override val restProtocol: String = "http"
    override val websocketProtocol: String = "ws"
    override val port: String = "8080"

}

class Prod : Environment() {

    override val host: String = "crowdcontroller.ddns.net"
    override val restProtocol: String = "https"
    override val websocketProtocol: String = "wss"
    override val port: String = "8443"

}
