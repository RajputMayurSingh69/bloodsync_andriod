package com.bloodsync.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.bloodsync.config.AppConfig
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

fun Application.configureCors() {
    install(CORS) {
        anyHost()
        allowCredentials = true
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-Request-ID")
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("success" to false, "message" to "An unexpected server error occurred.")
            )
            application.log.error("Uncaught exception", cause)
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                mapOf("success" to false, "message" to "Resource not found: ${call.request.httpMethod.value} ${call.request.uri}")
            )
        }
    }
}

fun Application.configureCallLogging() {
    install(CallLogging) {
        level = org.slf4j.event.Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
}

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("bank-jwt") {
            verifier(
                JWT.require(Algorithm.HMAC256(AppConfig.jwtSecret))
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("role").asString() == "blood_bank")
                    JWTPrincipal(credential.payload)
                else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("success" to false, "message" to "Unauthorized. Valid bank token required."))
            }
        }
    }
}
