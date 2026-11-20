package spring.study.admin.dto;

import java.time.LocalDateTime;

public record DeploymentInfo(String commit, boolean dirty, LocalDateTime startedAt) {
}
