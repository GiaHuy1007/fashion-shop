package cntt.giahuy.fashion_shop.common.exception;

import cntt.giahuy.fashion_shop.common.response.ApiResponse;
import cntt.giahuy.fashion_shop.common.response.ErrorDetail;
import cntt.giahuy.fashion_shop.common.response.FieldError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(ErrorDetail.from(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        null))
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorDetail.from(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        exception.getBindingResult().getFieldErrors()
                                .stream()
                                .map(fieldError
                                        -> FieldError.from(fieldError.getField(),
                                        fieldError.getRejectedValue(),
                                        fieldError.getDefaultMessage()))
                                .toList())
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(ErrorDetail.from(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        null))
                );
    }
}
