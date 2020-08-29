package com.example

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.locations.get
import io.ktor.routing.Route

fun Route.health() {
    get<Health> {
        call.response.status(HttpStatusCode.OK)
    }
}
