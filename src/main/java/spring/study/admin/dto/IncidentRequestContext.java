package spring.study.admin.dto;

public record IncidentRequestContext(String method, String path, String ipAddress) {}
