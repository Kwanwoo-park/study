package spring.study.admin.dto;

public record GitHubActivity(String id, String type, String actor, String pushedAt, String ref, String before, String after, String url) { }
