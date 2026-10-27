package cntt.giahuy.fashion_shop.common.response;

import java.util.List;

public record ErrorDetail(String code, String message, List<FieldError> fieldErrors) {
    public static ErrorDetail from(String code, String message, List<FieldError> fieldErrors) {
        return new ErrorDetail(code, message, fieldErrors);
    }
}
