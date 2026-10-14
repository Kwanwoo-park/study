package spring.study.admin.dto;

import java.util.List;

public record SystemProcessSnapshot(int totalCount, List<SystemProcessRow> processes) {}
