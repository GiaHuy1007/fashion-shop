package cntt.giahuy.fashion_shop.common.response;

public record FieldError(
        String field,
        Object rejectedValue,
        String message
) {
    public static FieldError from(String field, Object rejectedValue, String message) {
        return new FieldError(field, rejectedValue, message);
    }
}