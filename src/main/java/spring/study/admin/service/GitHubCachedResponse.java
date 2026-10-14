package spring.study.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import java.time.Instant;

record GitHubCachedResponse(JsonNode body, HttpHeaders headers, Instant fetchedAt) { }
