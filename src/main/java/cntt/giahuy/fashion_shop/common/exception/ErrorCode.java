package cntt.giahuy.fashion_shop.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    VALIDATION_ERROR("", "", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("", "", HttpStatus.INTERNAL_SERVER_ERROR);
    private final String code;
    private final String message;
    private final HttpStatus status;
}
