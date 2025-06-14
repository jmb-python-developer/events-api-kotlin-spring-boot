package com.jmb.events_api.sync.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal class DateRangeTest {

    @Test
    fun `should create DateRange when from is before to`() {
        val from = LocalDateTime.now()
        val to = from.plusDays(1)
        val range = DateRange(from, to)
        assertEquals(from, range.from)
        assertEquals(to, range.to)
    }

    @Test
    fun `should throw exception when from is not before to`() {
        val from = LocalDateTime.now()
        val to = from
        assertThrows<IllegalArgumentException> {
            DateRange(from, to)
        }
    }

    @Test
    fun `overlapsWith returns true for overlapping ranges`() {
        val from1 = LocalDateTime.now()
        val to1 = from1.plusDays(2)
        val from2 = from1.plusDays(1)
        val to2 = from2.plusDays(2)
        val range1 = DateRange(from1, to1)
        val range2 = DateRange(from2, to2)
        assertTrue(range1.overlapsWith(range2))
        assertTrue(range2.overlapsWith(range1))
    }

    @Test
    fun `overlapsWith returns false for non-overlapping ranges`() {
        val from1 = LocalDateTime.now()
        val to1 = from1.plusDays(1)
        val from2 = to1.plusDays(1)
        val to2 = from2.plusDays(1)
        val range1 = DateRange(from1, to1)
        val range2 = DateRange(from2, to2)
        assertFalse(range1.overlapsWith(range2))
        assertFalse(range2.overlapsWith(range1))
    }

    @Test
    fun `isEquivalentTo returns true for ranges equal up to minute precision`() {
        val from = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val to = from.plusHours(1).truncatedTo(ChronoUnit.MINUTES)
        val range1 = DateRange(from, to)
        val range2 = DateRange(from.plusSeconds(30), to.plusSeconds(30))
        assertTrue(range1.isEquivalentTo(range2))
    }

    @Test
    fun `isEquivalentTo returns false for ranges differing in minutes`() {
        val from1 = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val to1 = from1.plusHours(1).truncatedTo(ChronoUnit.MINUTES)
        val from2 = from1.plusMinutes(1)
        val to2 = to1.plusMinutes(1)
        val range1 = DateRange(from1, to1)
        val range2 = DateRange(from2, to2)
        assertFalse(range1.isEquivalentTo(range2))
    }

    @Test
    fun `isInPeriodRange returns true when date is within range`() {
        val now = LocalDateTime.now()
        val from = now.minusDays(1)
        val to = now.plusDays(1)
        val instant = now.atZone(ZoneId.systemDefault()).toInstant()
        assertTrue(DateRange.isInPeriodRange(instant, from, to))
    }

    @Test
    fun `isInPeriodRange returns false when date is before range`() {
        val now = LocalDateTime.now()
        val from = now.plusDays(1)
        val to = now.plusDays(2)
        val instant = now.atZone(ZoneId.systemDefault()).toInstant()
        assertFalse(DateRange.isInPeriodRange(instant, from, to))
    }

    @Test
    fun `isInPeriodRange returns false when date is after range`() {
        val now = LocalDateTime.now()
        val from = now.minusDays(2)
        val to = now.minusDays(1)
        val instant = now.atZone(ZoneId.systemDefault()).toInstant()
        assertFalse(DateRange.isInPeriodRange(instant, from, to))
    }
}
