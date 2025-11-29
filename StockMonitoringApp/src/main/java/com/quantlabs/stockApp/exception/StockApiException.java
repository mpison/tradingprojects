package com.quantlabs.stockApp.exception;

/**
 * Custom exception for stock API related errors with detailed error classification
 */
public class StockApiException extends Exception {
    private final ErrorType errorType;
    private final int httpStatusCode;
    private final String apiName;
    private final String requestDetails;

    public enum ErrorType {
        NETWORK_ERROR,
        AUTHENTICATION_ERROR,
        RATE_LIMIT_EXCEEDED,
        INVALID_REQUEST,
        PARSE_ERROR,
        API_UNAVAILABLE,
        DATA_NOT_FOUND,
        UNKNOWN_ERROR
    }

    // Basic constructor
    public StockApiException(String message) {
        this(message, ErrorType.UNKNOWN_ERROR, null, 0, null);
    }

    // Constructor with error type
    public StockApiException(String message, ErrorType errorType) {
        this(message, errorType, null, 0, null);
    }

    // Constructor with error type and cause
    public StockApiException(String message, ErrorType errorType, Throwable cause) {
        this(message, errorType, cause, 0, null);
    }

    // Full constructor
    public StockApiException(String message, ErrorType errorType, Throwable cause, 
                           int httpStatusCode, String apiName) {
        this(message, errorType, cause, httpStatusCode, apiName, null);
    }

    // Most complete constructor
    public StockApiException(String message, ErrorType errorType, Throwable cause,
                           int httpStatusCode, String apiName, String requestDetails) {
        super(message, cause);
        this.errorType = errorType;
        this.httpStatusCode = httpStatusCode;
        this.apiName = apiName;
        this.requestDetails = requestDetails;
    }

    // Getters
    public ErrorType getErrorType() {
        return errorType;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getApiName() {
        return apiName;
    }

    public String getRequestDetails() {
        return requestDetails;
    }

    // Helper methods for common exception types
    public static StockApiException networkError(String apiName, Throwable cause) {
        return new StockApiException(
            "Network error while accessing " + apiName + ": " + cause.getMessage(),
            ErrorType.NETWORK_ERROR,
            cause,
            0,
            apiName
        );
    }

    public static StockApiException rateLimitExceeded(String apiName, int statusCode) {
        return new StockApiException(
            "API rate limit exceeded for " + apiName,
            ErrorType.RATE_LIMIT_EXCEEDED,
            null,
            statusCode,
            apiName
        );
    }

    public static StockApiException parseError(String apiName, String responseData, Throwable cause) {
        return new StockApiException(
            "Failed to parse response from " + apiName + ": " + cause.getMessage(),
            ErrorType.PARSE_ERROR,
            cause,
            0,
            apiName,
            responseData
        );
    }

    public static StockApiException invalidRequest(String apiName, String requestDetails) {
        return new StockApiException(
            "Invalid request to " + apiName,
            ErrorType.INVALID_REQUEST,
            null,
            0,
            apiName,
            requestDetails
        );
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("StockApiException{");
        sb.append("errorType=").append(errorType);
        if (apiName != null) {
            sb.append(", apiName='").append(apiName).append('\'');
        }
        if (httpStatusCode > 0) {
            sb.append(", httpStatusCode=").append(httpStatusCode);
        }
        sb.append(", message='").append(getMessage()).append('\'');
        if (requestDetails != null) {
            sb.append(", requestDetailsLength=").append(requestDetails.length());
        }
        sb.append('}');
        return sb.toString();
    }
}