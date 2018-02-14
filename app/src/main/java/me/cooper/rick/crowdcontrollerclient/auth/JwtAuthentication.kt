package me.cooper.rick.crowdcontrollerclient.auth

import me.cooper.rick.crowdcontrollerapi.dto.Token
import org.springframework.http.HttpAuthentication

class JwtAuthentication(val token: String): HttpAuthentication() {
    override fun getHeaderValue(): String = "Bearer $token"
}