package com.example

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.routing.routing
import io.ktor.serialization.DefaultJsonConfiguration
import io.ktor.serialization.json
import kotlinx.serialization.json.Json
import java.util.logging.Logger

@Location("/health")
class Health

val logger: Logger = Logger.getLogger("ktor-r8")

fun Application.module() {
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        json(Json(DefaultJsonConfiguration), ContentType.Application.Json)
    }
    install(Locations)
    routing {
        health()
    }
    logger.info("Running")
}
