package spring.study.admin.dto;

import org.springframework.core.io.Resource;

public record Download(AdminFileResponseDto metadata, Resource resource) { }
