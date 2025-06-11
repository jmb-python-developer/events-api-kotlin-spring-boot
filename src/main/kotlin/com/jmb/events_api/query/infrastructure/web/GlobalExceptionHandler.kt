package com.jmb.events_api.query.infrastructure.web

import com.jmb.events_api.query.infrastructure.web.dto.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

/**
 * Global exception handler for consistent API error responses
 * Catches Spring framework exceptions before they reach controllers
 */
@ControllerAdvice
class GlobalExceptionHandler {

    /**
     * Handle missing request parameters (e.g., missing starts_at or ends_at)
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(ex: MissingServletRequestParameterException): ResponseEntity<ApiResponse> {
        val response = ApiResponse.error(
            code = "MISSING_PARAMETER",
            message = "${ex.parameterName} parameter is required"
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    /**
     * Handle invalid parameter types (e.g., non-string values)
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: org.springframework.web.method.annotation.MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse> {
        val response = ApiResponse.error(
            code = "INVALID_PARAMETER_TYPE",
            message = "Invalid value for parameter ${ex.name}"
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    /**
     * Fallback for any other unexpected exceptions
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ApiResponse> {
        val response = ApiResponse.error(
            code = "INTERNAL_ERROR",
            message = "An unexpected error occurred"
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}