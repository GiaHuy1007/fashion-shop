package cntt.giahuy.fashion_shop.common.response;

import java.util.List;

public record CursorResponse<T> (List<T> items, String nextCursor, boolean hasMore) {
}
