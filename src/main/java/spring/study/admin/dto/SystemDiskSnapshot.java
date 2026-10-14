package spring.study.admin.dto;

import java.util.List;

public record SystemDiskSnapshot(List<SystemDiskRoot> roots, String root, String path, int page,
                           int pageSize, int totalCount, List<SystemFileRow> entries) {}
