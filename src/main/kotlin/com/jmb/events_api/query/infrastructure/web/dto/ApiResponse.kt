package com.jmb.events_api.query.infrastructure.web.dto

import com.jmb.events_api.query.application.dto.EventResponseDto

/**
 * Standard API response wrapper following the Fever API specification
 */
data class ApiResponse(
    val error: ErrorResponse? = null,
    val data: DataResponse? = null
) {
    companion object {
        fun success(events: List<EventResponseDto>): ApiResponse {
            return ApiResponse(
                error = null,
                data = DataResponse(events = events)
            )
        }

        fun error(code: String, message: String): ApiResponse {
            return ApiResponse(
                error = ErrorResponse(code, message),
                data = null
            )
        }
    }
}

data class ErrorResponse(
    val code: String,
    val message: String
)

data class DataResponse(
    val events: List<EventResponseDto>
)