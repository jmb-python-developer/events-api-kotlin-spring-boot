package com.jmb.events_api.query.infrastructure.web.validation

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Input validation logic for search parameters
 */
@Component
class SearchValidation {

    fun validateSearchParams(startsAt: String, endsAt: String): ValidationResult {
        // Validate date format
        val startDate = try {
            LocalDate.parse(startsAt)
        } catch (e: DateTimeParseException) {
            return ValidationResult.invalid("Invalid starts_at date format. Expected: YYYY-MM-DD")
        }

        val endDate = try {
            LocalDate.parse(endsAt)
        } catch (e: DateTimeParseException) {
            return ValidationResult.invalid("Invalid ends_at date format. Expected: YYYY-MM-DD")
        }

        // Validate date range logic
        if (startDate.isAfter(endDate)) {
            return ValidationResult.invalid("starts_at cannot be after ends_at")
        }

        // Validate reasonable date range (optional business rule)
        val daysDifference = java.time.Period.between(startDate, endDate).days
        if (daysDifference > 365) {
            return ValidationResult.invalid("Date range cannot exceed 365 days")
        }

        return ValidationResult.valid()
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun valid() = ValidationResult(true)
        fun invalid(message: String) = ValidationResult(false, message)
    }
}