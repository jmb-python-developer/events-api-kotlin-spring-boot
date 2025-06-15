package com.jmb.events_api.sync.infrastructure.external.exception

/**
 * Domain exception for provider-related errors.
 */
class ProviderApiException(
    message: String,
    cause: Throwable? = null,
    val errorType: ErrorType = ErrorType.UNKNOWN
) : Exception(message, cause) {

    enum class ErrorType {
        NETWORK_ERROR,
        PARSE_ERROR,
        INVALID_RESPONSE,
        HTTP_ERROR,
        CIRCUIT_BREAKER_OPEN,
        TIMEOUT,
        UNEXPECTED_ERROR,
        UNKNOWN
    }

    companion object {
        fun networkError(cause: Throwable): ProviderApiException {
            return ProviderApiException(
                "Network error while fetching from provider: ${cause.message}",
                cause,
                ErrorType.NETWORK_ERROR
            )
        }

        fun parseError(cause: Throwable): ProviderApiException {
            return ProviderApiException(
                "Failed to parse provider response: ${cause.message}",
                cause,
                ErrorType.PARSE_ERROR
            )
        }

        fun invalidResponse(message: String): ProviderApiException {
            return ProviderApiException(
                "Invalid provider response: $message",
                null,
                ErrorType.INVALID_RESPONSE
            )
        }

        fun httpError(statusCode: Int, responseBody: String): ProviderApiException {
            return ProviderApiException(
                "HTTP error $statusCode from provider. Response: ${responseBody.take(200)}",
                null,
                ErrorType.HTTP_ERROR
            )
        }

        fun circuitBreakerOpen(message: String): ProviderApiException {
            return ProviderApiException(
                message,
                null,
                ErrorType.CIRCUIT_BREAKER_OPEN
            )
        }

        fun timeout(cause: Throwable): ProviderApiException {
            return ProviderApiException(
                "Timeout while fetching from provider: ${cause.message}",
                cause,
                ErrorType.TIMEOUT
            )
        }

        fun unexpectedError(cause: Throwable): ProviderApiException {
            return ProviderApiException(
                "Unexpected error while fetching from provider: ${cause.message}",
                cause,
                ErrorType.UNEXPECTED_ERROR
            )
        }
    }
}
