package cntt.giahuy.fashion_shop.common.response;

import org.slf4j.MDC;

import java.time.Instant;

public record ApiResponse<T> (boolean success, T data, ErrorDetail error, String traceId, Instant timestamp) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(
                true,
                data,
                null,
                MDC.get("traceId"),
                Instant.now()
        );
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(
                true,
                null,
                null,
                MDC.get("traceId"),
                Instant.now()
        );
    }

    public static <T> ApiResponse<T> error(ErrorDetail details) {
        return new ApiResponse<>(
                false,
                null,
                ErrorDetail.from(details.code(), details.message(), details.fieldErrors()), MDC.get("traceId"),
                Instant.now()
        );
    }
}
