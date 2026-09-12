package spring.study.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import spring.study.admin.dto.GitHubHistoryResponse;
import spring.study.admin.dto.GitHubHistoryResponse.Activity;
import spring.study.admin.dto.GitHubHistoryResponse.Commit;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class GitHubHistoryService {
    private static final int PAGE_SIZE = 20;
    private static final Pattern NEXT_LINK = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
    private final RestTemplate client;
    private final String owner;
    private final String repository;
    private final String branch;
    private final String token;
    private final Clock clock;
    private final Map<URI, CachedResponse> cache = new LinkedHashMap<>();
    private Instant retryAt = Instant.EPOCH;
    private String unavailableMessage;

    @Autowired
    public GitHubHistoryService(@Qualifier("gitHubHistoryRestTemplate") RestTemplate client,
            @Value("${admin.github.owner:Kwanwoo-park}") String owner,
            @Value("${admin.github.repository:study}") String repository,
            @Value("${admin.github.branch:main}") String branch,
            @Value("${admin.github.token:${ADMIN_GITHUB_TOKEN:}}") String token) {
        this(client, owner, repository, branch, token, Clock.systemUTC());
    }

    GitHubHistoryService(RestTemplate client, String owner, String repository, String branch, String token, Clock clock) {
        if (!validName(owner) || !validName(repository) || branch == null || branch.isBlank()
                || branch.length() > 255 || branch.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("GitHub 저장소 및 브랜치 설정을 확인해 주세요");
        }
        this.client = client;
        this.owner = owner;
        this.repository = repository;
        this.branch = branch;
        this.token = token == null ? "" : token.trim();
        this.clock = clock;
    }

    public GitHubHistoryResponse<Commit> commits(int page) {
        if (page < 1 || page > 1000) throw badRequest("페이지는 1~1000 범위여야 합니다");
        URI uri = baseUri("commits").queryParam("sha", "{branch}")
                .queryParam("page", page).encode().buildAndExpand(branch).toUri();
        CachedResponse response = request(uri);
        List<Commit> entries = new ArrayList<>();
        for (JsonNode item : response.body()) {
            JsonNode commit = item.path("commit");
            String sha = text(item, "sha");
            entries.add(new Commit(sha, text(commit, "message"), text(commit.path("author"), "name"),
                    text(commit.path("author"), "date"), text(commit.path("committer"), "date"), commitUrl(sha)));
        }
        String next = nextParameter(response, uri, "page");
        if (!String.valueOf(page + 1).equals(next) || page == 1000) next = null;
        return new GitHubHistoryResponse<>(owner + "/" + repository, branch, List.copyOf(entries), next, response.fetchedAt());
    }

    public GitHubHistoryResponse<Activity> activity(String cursor) {
        UriComponentsBuilder builder = baseUri("activity").queryParam("ref", "{branch}").queryParam("direction", "desc");
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("branch", branch);
        if (cursor != null && !cursor.isEmpty()) {
            String[] parts = cursor.split(":", 2);
            if (parts.length != 2 || !(parts[0].equals("before") || parts[0].equals("after")) || !validCursor(parts[1])) {
                throw badRequest("올바르지 않은 조회 위치입니다");
            }
            builder.queryParam(parts[0], "{cursor}");
            parameters.put("cursor", parts[1]);
        }
        URI uri = builder.encode().buildAndExpand(parameters).toUri();
        CachedResponse response = request(uri);
        List<Activity> entries = new ArrayList<>();
        for (JsonNode item : response.body()) {
            String before = text(item, "before");
            String after = text(item, "after");
            // Keep both documented field names and the older activity representation readable.
            entries.add(new Activity(text(item, "id"), first(text(item, "activity_type"), text(item, "push_type")),
                    first(text(item.path("actor"), "login"), text(item.path("pusher"), "login")),
                    first(text(item, "timestamp"), text(item, "pushed_at")), text(item, "ref"), before, after,
                    compareUrl(before, after)));
        }
        String next = null;
        for (String direction : List.of("before", "after")) {
            String value = nextParameter(response, uri, direction);
            if (validCursor(value)) {
                next = direction + ":" + value;
                break;
            }
        }
        if (next != null && next.equals(cursor)) next = null;
        return new GitHubHistoryResponse<>(owner + "/" + repository, branch, List.copyOf(entries), next, response.fetchedAt());
    }

    private UriComponentsBuilder baseUri(String resource) {
        // The browser can choose a page, never a host, repository or arbitrary URL.
        return UriComponentsBuilder.fromHttpUrl("https://api.github.com")
                .pathSegment("repos", owner, repository, resource).queryParam("per_page", PAGE_SIZE);
    }

    private synchronized CachedResponse request(URI uri) {
        Instant now = clock.instant();
        cache.entrySet().removeIf(entry -> !entry.getValue().fetchedAt().plusSeconds(60).isAfter(now));
        CachedResponse cached = cache.get(uri);
        if (cached != null) return cached;
        if (retryAt.isAfter(now)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, unavailableMessage);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
        headers.set("X-GitHub-Api-Version", "2026-03-10");
        headers.set("User-Agent", "Kwanwoo-Admin-History");
        if (!token.isBlank()) headers.setBearerAuth(token);
        try {
            ResponseEntity<JsonNode> response = client.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || !response.getBody().isArray()) {
                throw unavailable("GitHub에서 올바른 내역을 받지 못했습니다. 잠시 후 다시 시도해 주세요", 15);
            }
            CachedResponse result = new CachedResponse(response.getBody(), response.getHeaders(), clock.instant());
            if (cache.size() >= 100) cache.remove(cache.keySet().iterator().next());
            cache.put(uri, result);
            return result;
        } catch (RestClientResponseException error) {
            int status = error.getStatusCode().value();
            if (status == 409 && uri.getPath().endsWith("/commits")) {
                // GitHub returns 409 for a newly created, empty repository.
                CachedResponse empty = new CachedResponse(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode(),
                        HttpHeaders.EMPTY, clock.instant());
                if (cache.size() >= 100) cache.remove(cache.keySet().iterator().next());
                cache.put(uri, empty);
                return empty;
            }
            if (status == 429 || (status == 403 && error.getResponseHeaders() != null
                    && ("0".equals(error.getResponseHeaders().getFirst("X-RateLimit-Remaining"))
                    || error.getResponseHeaders().containsKey("Retry-After")))) {
                throw unavailable("GitHub API 조회 한도에 도달했습니다. 잠시 후 다시 시도해 주세요", retrySeconds(error.getResponseHeaders()));
            }
            if (status == 401 || status == 403 || status == 404) {
                throw unavailable("GitHub 저장소·브랜치 또는 서버의 GitHub 토큰 읽기 권한을 확인해 주세요", 30);
            }
            // Never expose GitHub's raw error body, request headers or token to clients/logs.
            throw unavailable("GitHub 내역 조회에 실패했습니다. 잠시 후 다시 시도해 주세요", 15);
        } catch (RestClientException error) {
            throw unavailable("GitHub에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요", 15);
        }
    }

    private ResponseStatusException unavailable(String message, long seconds) {
        unavailableMessage = message;
        retryAt = clock.instant().plusSeconds(seconds);
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    private long retrySeconds(HttpHeaders headers) {
        long seconds = 60;
        if (headers != null) {
            try { seconds = Math.max(seconds, Long.parseLong(headers.getFirst("Retry-After"))); }
            catch (NumberFormatException ignored) { }
            try { seconds = Math.max(seconds, Long.parseLong(headers.getFirst("X-RateLimit-Reset")) - clock.instant().getEpochSecond()); }
            catch (NumberFormatException ignored) { }
        }
        return Math.min(seconds, 86400);
    }

    private String nextParameter(CachedResponse response, URI requestUri, String name) {
        var matcher = NEXT_LINK.matcher(String.join(",", response.headers().getOrEmpty(HttpHeaders.LINK)));
        if (!matcher.find()) return null;
        try {
            URI next = URI.create(matcher.group(1));
            if (!"https".equals(next.getScheme()) || !"api.github.com".equals(next.getHost())
                    || !requestUri.getPath().equals(next.getPath())) return null;
            String value = UriComponentsBuilder.fromUri(next).build().getQueryParams().getFirst(name);
            return value == null ? null : UriUtils.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private String commitUrl(String sha) {
        return validSha(sha) ? "https://github.com/" + owner + "/" + repository + "/commit/" + sha : null;
    }

    private String compareUrl(String before, String after) {
        if (validSha(before) && validSha(after)) {
            return "https://github.com/" + owner + "/" + repository + "/compare/" + before + "..." + after;
        }
        return commitUrl(after);
    }

    private static boolean validSha(String sha) { return sha != null && sha.matches("[0-9a-fA-F]{40,64}") && !sha.matches("0+"); }
    private static boolean validName(String name) { return name != null && name.matches("[A-Za-z0-9_.-]{1,100}") && !name.equals(".") && !name.equals(".."); }
    private static boolean validCursor(String cursor) { return cursor != null && !cursor.isBlank() && cursor.length() <= 512 && cursor.chars().noneMatch(Character::isISOControl); }
    private static String text(JsonNode node, String field) { return node.path(field).asText(""); }
    private static String first(String value, String fallback) { return value.isBlank() ? fallback : value; }
    private static ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }

    private record CachedResponse(JsonNode body, HttpHeaders headers, Instant fetchedAt) { }
}
