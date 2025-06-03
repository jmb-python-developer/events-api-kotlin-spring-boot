package com.jmb.events_api.sync.domain.model

import java.time.Instant
import java.time.LocalDateTime

data class DateRange(
    val sellFrom: LocalDateTime,
    val sellTo: LocalDateTime,
) {
    //validations
    init {
        require(sellFrom.isBefore(sellTo))
    }

    fun overlapsWith(other: DateRange): Boolean {
        return this.sellFrom < other.sellTo && this.sellTo >= other.sellFrom
    }

    companion object {
        fun isInPeriodRange(date: Instant, sellFrom: LocalDateTime, sellTo: LocalDateTime): Boolean {
            return date.isAfter(sellFrom.atZone(java.time.ZoneId.systemDefault()).toInstant()) &&
                    date.isBefore(sellTo.atZone(java.time.ZoneId.systemDefault()).toInstant())
        }
    }
}