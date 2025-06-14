package com.jmb.events_api.sync.domain.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class DateRange(
    val from: LocalDateTime,
    val to: LocalDateTime,
) {
    //validations
    init {
        require(from.isBefore(to))
    }

    fun overlapsWith(other: DateRange): Boolean {
        return this.from < other.to && this.to >= other.from
    }

    /**
     * Check if two date ranges are equivalent (ignore second-level differences)
     */
    fun isEquivalentTo(other: DateRange): Boolean {
        return this.from.truncatedTo(ChronoUnit.MINUTES) ==
                other.from.truncatedTo(ChronoUnit.MINUTES) &&
                this.to.truncatedTo(ChronoUnit.MINUTES) ==
                other.to.truncatedTo(ChronoUnit.MINUTES)
    }

    companion object {
        fun isInPeriodRange(date: Instant, sellFrom: LocalDateTime, sellTo: LocalDateTime): Boolean {
            return date.isAfter(sellFrom.atZone(java.time.ZoneId.systemDefault()).toInstant()) &&
                    date.isBefore(sellTo.atZone(java.time.ZoneId.systemDefault()).toInstant())
        }
    }
}