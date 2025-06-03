package com.jmb.events_api.sync.infrastructure.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
class EventApi {

    @GetMapping("/hello")
    fun hello(): String {
        return "Hello from Event API"
    }
}