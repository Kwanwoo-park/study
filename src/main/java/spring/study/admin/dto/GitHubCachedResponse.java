package spring.study.admin.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import java.time.Instant;

public record GitHubCachedResponse(JsonNode body, HttpHeaders headers, Instant fetchedAt) { }
