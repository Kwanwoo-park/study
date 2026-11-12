package spring.study.memo.dto;

import java.util.List;

public record AdminMemoPageDto(List<AdminMemoSummaryDto> content, int page, int totalPages,
                               long matchedCount, long totalCount, long pinnedCount) {
}
