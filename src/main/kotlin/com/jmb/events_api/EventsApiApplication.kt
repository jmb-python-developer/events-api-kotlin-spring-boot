package com.jmb.events_api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EventsApiApplication

fun main(args: Array<String>) {
	runApplication<EventsApiApplication>(*args)
}
