package spring.study.admin.dto;

import java.nio.file.Path;

public record SystemDiagnosticRootPath(String id, String label, Path path) {}
