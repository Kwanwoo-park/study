package spring.study.admin.dto;

public record GitHubCommit(String sha, String message, String author, String authoredAt, String committedAt, String url) { }
