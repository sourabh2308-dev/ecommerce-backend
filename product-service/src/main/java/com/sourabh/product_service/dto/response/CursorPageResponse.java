package com.sourabh.product_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CursorPageResponse<T> {

    private List<T> content;

    private int size;

    private long totalElements;

    private boolean hasNext;

    private String nextCursor;
}
