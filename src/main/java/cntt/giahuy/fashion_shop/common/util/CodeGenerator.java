package cntt.giahuy.fashion_shop.common.util;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@UtilityClass
public class CodeGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    /**
     * Sinh mã đơn hàng theo chuẩn: FS + yyMMdd + 5 ký tự ngẫu nhiên (VD: FS261029A7K3Q)
     */
    public String generateOrderCode() {
        String datePart = LocalDate.now(VN_ZONE).format(DATE_FORMATTER);
        return "FS" + datePart + randomAlphanumeric(5);
    }

    /**
     * Sinh mã tham chiếu giao dịch thanh toán (txn_ref) gửi sang cổng thanh toán
     */
    public String generateTxnRef(String prefix) {
        String normalizedPrefix = (prefix == null || prefix.isBlank()) ? "TXN" : prefix.trim().toLowerCase(Locale.ROOT);
        String timePart = LocalDateTime.now(VN_ZONE).format(DATETIME_FORMATTER);
        return normalizedPrefix + timePart + randomAlphanumeric(6);
    }

    public String generateTxnRef() {
        return generateTxnRef("TXN");
    }

    /**
     * Sinh chuỗi chữ-số in hoa ngẫu nhiên (loại bỏ I, O dễ nhầm với 1, 0)
     */
    public String randomAlphanumeric(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Sinh mã OTP gồm các chữ số
     */
    public String generateNumericOtp(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }
        return sb.toString();
    }
}
