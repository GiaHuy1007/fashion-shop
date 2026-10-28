package cntt.giahuy.fashion_shop.common.util;

import lombok.experimental.UtilityClass;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@UtilityClass
public class SlugUtils {
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS_PATTERN = Pattern.compile("(^-+|-+$)");

    public String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String normalized = input.trim()
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replace('Đ', 'd');

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        String slug = NON_ALPHANUMERIC_PATTERN.matcher(withoutDiacritics).replaceAll("-");

        return EDGE_HYPHENS_PATTERN.matcher(slug).replaceAll("");
    }

    public String toSlug(String input, String suffix) {
        String baseSlug = toSlug(input);
        String suffixSlug = toSlug(suffix);
        if (suffixSlug.isEmpty()) {
            return baseSlug;
        }
        if (baseSlug.isEmpty()) {
            return suffixSlug;
        }
        return baseSlug + "-" + suffixSlug;
    }
}
