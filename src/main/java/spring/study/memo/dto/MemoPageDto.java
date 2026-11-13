package spring.study.memo.dto;

import java.util.List;

public record MemoPageDto(List<MemoSummaryDto> content, int page, int totalPages,
                               long matchedCount, long totalCount, long pinnedCount) {
}
