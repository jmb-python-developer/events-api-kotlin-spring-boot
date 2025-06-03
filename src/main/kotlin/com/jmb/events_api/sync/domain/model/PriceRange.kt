package com.jmb.events_api.sync.domain.model

import java.math.BigDecimal

class PriceRange(val min: BigDecimal, val max: BigDecimal) {

    //Validations
    init {
        require(min >= BigDecimal.ZERO) { "Min price cannot be negative" }
        require(max >= BigDecimal.ZERO) { "Max price cannot be negative" }
        require(min <= max) { "Min price cannot be greater than max price" }
    }

    fun isWithinBudget(budget: BigDecimal): Boolean = max <= budget

    fun averagePrice(): BigDecimal = (min + max).divide(BigDecimal.valueOf(2))

    fun priceSpread(): BigDecimal = max - min

    fun isFixedPrice(): Boolean = min == max

    fun contains(price: BigDecimal): Boolean = price in min..max

    // Factory methods for common scenarios
    companion object {
        fun free(): PriceRange = PriceRange(BigDecimal.ZERO, BigDecimal.ZERO)
        fun fixed(price: BigDecimal): PriceRange = PriceRange(price, price)
    }
}