package spring.study.admin.dto;

import java.time.Instant;

public record SystemFileRow(String name, boolean directory, long sizeBytes, Instant modifiedAt) {}
