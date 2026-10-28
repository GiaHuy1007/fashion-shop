package cntt.giahuy.fashion_shop.common.util;

import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@UtilityClass
public class HmacUtils {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String HMAC_SHA512 = "HmacSHA512";

    /**
     * Ký HMAC-SHA256 trả về chuỗi hex chữ thường (dùng cho MoMo)
     */
    public String hmacSha256(String secretKey, String data) {
        return computeHmacHex(HMAC_SHA256, secretKey, data);
    }

    /**
     * Ký HMAC-SHA512 trả về chuỗi hex chữ thường (dùng cho VNPay vnp_SecureHash)
     */
    public String hmacSha512(String secretKey, String data) {
        return computeHmacHex(HMAC_SHA512, secretKey, data);
    }

    /**
     * So sánh chữ ký theo thời gian hằng (constant-time) để chống timing attack
     */
    public boolean verify(String expectedHex, String actualHex) {
        if (expectedHex == null || actualHex == null) {
            return false;
        }
        byte[] expectedBytes = expectedHex.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actualHex.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private String computeHmacHex(String algorithm, String secretKey, String data) {
        if (secretKey == null || data == null) {
            throw new IllegalArgumentException("Secret key and data must not be null");
        }
        try {
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), algorithm);
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to calculate " + algorithm + " signature", e);
        }
    }
}
