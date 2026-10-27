package cntt.giahuy.fashion_shop.common.response;

public record ErrorDetail(String code, String message, Object details) {
    public static ErrorDetail from(String code, String message, Object details) {
        return new ErrorDetail(code, message, details);
    }
}
