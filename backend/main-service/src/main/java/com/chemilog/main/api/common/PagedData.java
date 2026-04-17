package com.chemilog.main.api.common;

import java.util.List;

public record PagedData<T>(
        List<T> items,
        PageInfo pageInfo
) {
}
