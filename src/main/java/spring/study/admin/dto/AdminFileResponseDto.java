package spring.study.admin.dto;

import spring.study.admin.entity.AdminFile;

import java.time.LocalDateTime;

public record AdminFileResponseDto(String id, String originalFilename, long size, Long uploadedBy, LocalDateTime createdAt) {
    public static AdminFileResponseDto from(AdminFile file) {
        return new AdminFileResponseDto(file.getId(), file.getOriginalFilename(), file.getSize(), file.getUploadedBy(), file.getCreatedAt());
    }
}
