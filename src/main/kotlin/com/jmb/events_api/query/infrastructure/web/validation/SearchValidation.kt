package com.jmb.events_api.query.infrastructure.web.validation

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Component
class SearchValidation {

    // Custom infix function for chaining validations with fail-fast semantics
    private infix fun ValidationResult.and(nextValidation: () -> ValidationResult): ValidationResult {
        return if (this.isValid) nextValidation() else this
    }

    /**
     * Main validation method using clean infix chaining
     */
    fun validateSearchParams(startsAt: String?, endsAt: String?): ValidationResult {
        val params = SearchParams(startsAt, endsAt)

        return validateRequired(params) and
                { validateDateFormat(params.startsAt, "starts_at") } and
                { validateDateFormat(params.endsAt, "ends_at") } and
                { validateDateRange(params) } and
                { validateDateRangeSize(params) }
    }

    private fun validateRequired(params: SearchParams): ValidationResult = when {
        params.startsAt.isNullOrBlank() ->
            ValidationResult.invalid("MISSING_PARAMETER", "starts_at parameter is required")
        params.endsAt.isNullOrBlank() ->
            ValidationResult.invalid("MISSING_PARAMETER", "ends_at parameter is required")
        else -> ValidationResult.valid()
    }

    private fun validateDateFormat(date: String?, paramName: String): ValidationResult {
        return if (date.isNullOrBlank()) ValidationResult.valid()
        else try {
            LocalDate.parse(date)
            ValidationResult.valid()
        } catch (e: DateTimeParseException) {
            ValidationResult.invalid("INVALID_DATE_FORMAT",
                "Invalid $paramName date format. Expected: YYYY-MM-DD")
        }
    }

    private fun validateDateRange(params: SearchParams): ValidationResult {
        if (params.startsAt.isNullOrBlank() || params.endsAt.isNullOrBlank()) {
            return ValidationResult.valid()
        }

        return try {
            val startDate = LocalDate.parse(params.startsAt)
            val endDate = LocalDate.parse(params.endsAt)

            if (startDate.isAfter(endDate)) {
                ValidationResult.invalid("INVALID_DATE_RANGE", "starts_at cannot be after ends_at")
            } else {
                ValidationResult.valid()
            }
        } catch (e: DateTimeParseException) {
            ValidationResult.valid()
        }
    }

    private fun validateDateRangeSize(params: SearchParams): ValidationResult {
        if (params.startsAt.isNullOrBlank() || params.endsAt.isNullOrBlank()) {
            return ValidationResult.valid()
        }

        return try {
            val startDate = LocalDate.parse(params.startsAt)
            val endDate = LocalDate.parse(params.endsAt)
            val daysDifference = java.time.Period.between(startDate, endDate).days

            if (daysDifference > 365) {
                ValidationResult.invalid("DATE_RANGE_TOO_LARGE", "Date range cannot exceed 365 days")
            } else {
                ValidationResult.valid()
            }
        } catch (e: DateTimeParseException) {
            ValidationResult.valid()
        }
    }
}

/**
 * Data class to hold search parameters
 */
data class SearchParams(
    val startsAt: String?,
    val endsAt: String?
)

/**
 * Enhanced ValidationResult with error codes for better API responses
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorCode: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun valid() = ValidationResult(true)
        fun invalid(code: String, message: String) = ValidationResult(false, code, message)
    }
}