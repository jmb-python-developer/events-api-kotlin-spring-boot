package com.jmb.events_api.sync.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

class PriceRange(val min: BigDecimal, val max: BigDecimal) {

    //Validations
    init {
        require(min >= BigDecimal.ZERO) { "Min price cannot be negative" }
        require(max >= BigDecimal.ZERO) { "Max price cannot be negative" }
        require(min <= max) { "Min price cannot be greater than max price" }
    }

    fun isEquivalentTo(other: PriceRange): Boolean {
        return areEquivalent(this.min, other.min) && areEquivalent(this.max, other.max)
    }

    fun isWithinBudget(budget: BigDecimal): Boolean = max <= budget

    fun averagePrice(): BigDecimal = (min + max).divide(BigDecimal.valueOf(2))

    fun priceSpread(): BigDecimal = max - min

    fun isFixedPrice(): Boolean = min == max

    fun contains(price: BigDecimal): Boolean = price in min..max

    // Factory methods
    companion object {
        private fun areEquivalent(price1: BigDecimal, price2: BigDecimal): Boolean {
            // Round to 2 decimal places for comparison (standard for currency)
            val rounded1 = price1.setScale(2, RoundingMode.HALF_UP)
            val rounded2 = price2.setScale(2, RoundingMode.HALF_UP)
            return rounded1.compareTo(rounded2) == 0
        }
        fun free(): PriceRange = PriceRange(BigDecimal.ZERO, BigDecimal.ZERO)
        fun fixed(price: BigDecimal): PriceRange = PriceRange(price, price)
    }
}
