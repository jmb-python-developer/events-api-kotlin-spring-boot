package com.jmb.events_api.sync.infrastructure.external.exception

class ProviderApiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    companion object {
        fun networkError(cause: Throwable): ProviderApiException {
            return ProviderApiException("Network error while fetching from provider", cause)
        }

        fun parseError(cause: Throwable): ProviderApiException {
            return ProviderApiException("Failed to parse provider response", cause)
        }

        fun invalidResponse(message: String): ProviderApiException {
            return ProviderApiException("Invalid provider response: $message")
        }
    }
}