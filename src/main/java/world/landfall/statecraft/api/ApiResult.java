package world.landfall.statecraft.api;

import java.util.Optional;
import java.util.function.Function;

/**
 * Represents the result of an API operation.
 *
 * <p>This provides a type-safe way to handle both successful results and errors,
 * with detailed error classification for appropriate handling.
 *
 * @param <T> The type of the successful result
 */
public sealed interface ApiResult<T> {

    /**
     * Returns true if the operation was successful.
     */
    boolean isSuccess();

    /**
     * Returns true if the operation failed.
     */
    default boolean isFailure() {
        return !isSuccess();
    }

    /**
     * Gets the value if successful.
     *
     * @return Optional containing the value, or empty if failed
     */
    Optional<T> getValue();

    /**
     * Gets the value or throws if failed.
     *
     * @return The value
     * @throws IllegalStateException if the operation failed
     */
    T getOrThrow();

    /**
     * Gets the value or a default if failed.
     *
     * @param defaultValue The default value
     * @return The value or default
     */
    T getOrElse(T defaultValue);

    /**
     * Maps the value if successful.
     *
     * @param mapper The mapping function
     * @param <U>    The new type
     * @return A new result with the mapped value
     */
    <U> ApiResult<U> map(Function<T, U> mapper);

    /**
     * Gets the error type if failed.
     *
     * @return Optional containing the error type, or empty if successful
     */
    Optional<ErrorType> getErrorType();

    /**
     * Gets the error message if failed.
     *
     * @return Optional containing the error message, or empty if successful
     */
    Optional<String> getErrorMessage();

    /**
     * Checks if this failure should be retried.
     *
     * @return True if the operation should be retried
     */
    boolean shouldRetry();

    /**
     * A successful result.
     */
    record Success<T>(T value) implements ApiResult<T> {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public Optional<T> getValue() {
            return Optional.ofNullable(value);
        }

        @Override
        public T getOrThrow() {
            return value;
        }

        @Override
        public T getOrElse(T defaultValue) {
            return value != null ? value : defaultValue;
        }

        @Override
        public <U> ApiResult<U> map(Function<T, U> mapper) {
            return new Success<>(mapper.apply(value));
        }

        @Override
        public Optional<ErrorType> getErrorType() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getErrorMessage() {
            return Optional.empty();
        }

        @Override
        public boolean shouldRetry() {
            return false;
        }
    }

    /**
     * A failed result.
     */
    record Failure<T>(ErrorType errorType, String message) implements ApiResult<T> {
        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<T> getValue() {
            return Optional.empty();
        }

        @Override
        public T getOrThrow() {
            throw new IllegalStateException("API operation failed: " + message);
        }

        @Override
        public T getOrElse(T defaultValue) {
            return defaultValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> ApiResult<U> map(Function<T, U> mapper) {
            return (ApiResult<U>) this;
        }

        @Override
        public Optional<ErrorType> getErrorType() {
            return Optional.of(errorType);
        }

        @Override
        public Optional<String> getErrorMessage() {
            return Optional.of(message);
        }

        @Override
        public boolean shouldRetry() {
            return errorType.isRetryable();
        }
    }

    /**
     * Types of errors that can occur during API operations.
     */
    enum ErrorType {
        /**
         * Network connectivity issues (connection refused, timeout, etc.)
         */
        NETWORK_ERROR(true),

        /**
         * Server returned 5xx error
         */
        SERVER_ERROR(true),

        /**
         * Authentication failed (401, 403)
         */
        AUTH_ERROR(false),

        /**
         * Resource not found (404)
         */
        NOT_FOUND(false),

        /**
         * Invalid request (400, 422)
         */
        VALIDATION_ERROR(false),

        /**
         * Rate limited (429)
         */
        RATE_LIMITED(true),

        /**
         * Unknown or unexpected error
         */
        UNKNOWN(false);

        private final boolean retryable;

        ErrorType(boolean retryable) {
            this.retryable = retryable;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }

    // Factory methods

    static <T> ApiResult<T> success(T value) {
        return new Success<>(value);
    }

    static <T> ApiResult<T> failure(ErrorType type, String message) {
        return new Failure<>(type, message);
    }

    static <T> ApiResult<T> networkError(String message) {
        return failure(ErrorType.NETWORK_ERROR, message);
    }

    static <T> ApiResult<T> serverError(String message) {
        return failure(ErrorType.SERVER_ERROR, message);
    }

    static <T> ApiResult<T> authError(String message) {
        return failure(ErrorType.AUTH_ERROR, message);
    }

    static <T> ApiResult<T> notFound(String message) {
        return failure(ErrorType.NOT_FOUND, message);
    }

    static <T> ApiResult<T> validationError(String message) {
        return failure(ErrorType.VALIDATION_ERROR, message);
    }
}
