package com.jmb.events_api.sync.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

internal class PriceRangeTest {

    @Test
    fun `constructor should throw if min is negative`() {
        val exception = assertThrows<IllegalArgumentException> {
            PriceRange(BigDecimal("-1.00"), BigDecimal("10.00"))
        }
        assertEquals("Min price cannot be negative", exception.message)
    }

    @Test
    fun `constructor should throw if max is negative`() {
        val exception = assertThrows<IllegalArgumentException> {
            PriceRange(BigDecimal("0.00"), BigDecimal("-10.00"))
        }
        assertEquals("Max price cannot be negative", exception.message)
    }

    @Test
    fun `constructor should throw if min greater than max`() {
        val exception = assertThrows<IllegalArgumentException> {
            PriceRange(BigDecimal("20.00"), BigDecimal("10.00"))
        }
        assertEquals("Min price cannot be greater than max price", exception.message)
    }

    @Test
    fun `isEquivalentTo should return true for equivalent ranges`() {
        val a = PriceRange(BigDecimal("10.005"), BigDecimal("20.004"))
        val b = PriceRange(BigDecimal("10.00"), BigDecimal("20.00"))
        //False as it rounds to two decimal places
        assertFalse(a.isEquivalentTo(b))
    }

    @Test
    fun `isEquivalentTo should return false for non-equivalent ranges`() {
        val a = PriceRange(BigDecimal("10.00"), BigDecimal("20.00"))
        val b = PriceRange(BigDecimal("10.01"), BigDecimal("20.00"))
        assertFalse(a.isEquivalentTo(b))
    }

    @Test
    fun `isWithinBudget should return true if max is less than or equal to budget`() {
        val range = PriceRange(BigDecimal("10.00"), BigDecimal("20.00"))
        assertTrue(range.isWithinBudget(BigDecimal("20.00")))
        assertTrue(range.isWithinBudget(BigDecimal("25.00")))
    }

    @Test
    fun `isWithinBudget should return false if max is greater than budget`() {
        val range = PriceRange(BigDecimal("10.00"), BigDecimal("20.00"))
        assertFalse(range.isWithinBudget(BigDecimal("19.99")))
    }

    @Test
    fun `averagePrice should return correct average`() {
        val range = PriceRange(BigDecimal("10.00"), BigDecimal("20.00"))
        assertEquals(BigDecimal("15.00"), range.averagePrice())
    }

    @Test
    fun `priceSpread should return correct spread`() {
        val range = PriceRange(BigDecimal("10.00"), BigDecimal("20.00"))
        assertEquals(BigDecimal("10.00"), range.priceSpread())
    }

    @Test
    fun `isFixedPrice should return true if min equals max`() {
        val range = PriceRange(BigDecimal("10.00"), BigDecimal("10.00"))
        assertTrue(range.isFixedPrice())
    }

    @Test
    fun `isFixedPrice should return false if min does not equal max`() {
        val range = PriceRange(BigDecimal("10.00"), BigDecimal("20.00"))
        assertFalse(range.isFixedPrice())
    }

    @Test
    fun `contains should return true if price is within range`() {
        val range = PriceRange(BigDecimal("10.00"), BigDecimal("20.00"))
        assertTrue(range.contains(BigDecimal("15.00")))
        assertTrue(range.contains(BigDecimal("10.00")))
        assertTrue(range.contains(BigDecimal("20.00")))
    }

    @Test
    fun `contains should return false if price is outside range`() {
        val range = PriceRange(BigDecimal("10.00"), BigDecimal("20.00"))
        assertFalse(range.contains(BigDecimal("9.99")))
        assertFalse(range.contains(BigDecimal("20.01")))
    }

    @Test
    fun `free factory should return zero min and max`() {
        val range = PriceRange.free()
        assertEquals(BigDecimal.ZERO, range.min)
        assertEquals(BigDecimal.ZERO, range.max)
        assertTrue(range.isFixedPrice())
    }

    @Test
    fun `fixed factory should return min and max equal to price`() {
        val price = BigDecimal("42.42")
        val range = PriceRange.fixed(price)
        assertEquals(price, range.min)
        assertEquals(price, range.max)
        assertTrue(range.isFixedPrice())
    }
}
