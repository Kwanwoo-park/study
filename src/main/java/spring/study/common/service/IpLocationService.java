package spring.study.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import spring.study.common.dto.IpLocationResponse;

import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
public class IpLocationService {
    private static final String CACHE_PREFIX = "ip-location:";
    private static final Duration CACHE_TTL = Duration.ofDays(7);
    private static final Duration FAILURE_CACHE_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, String> redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiToken;

    public IpLocationService(
            RedisTemplate<String, String> redisTemplate,
            @Qualifier("ipLocationRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${ip-location.base-url:${IP_LOCATION_BASE_URL:https://ipinfo.io}}") String baseUrl,
            @Value("${ip-location.api-token:${IPINFO_TOKEN:}}") String apiToken
    ) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiToken = apiToken;
    }

    public IpLocationResponse find(String ipAddress) {
        String normalizedIp = normalize(ipAddress);
        if (normalizedIp == null) return IpLocationResponse.unknown();
        if (isInternal(normalizedIp)) return IpLocationResponse.internal();

        IpLocationResponse cached = readCache(normalizedIp);
        if (cached != null) return cached;

        try {
            URI uri = buildUri(normalizedIp);
            String responseBody = restTemplate.getForObject(uri, String.class);
            IpLocationResponse result = parse(responseBody);
            writeCache(normalizedIp, result, result.available() ? CACHE_TTL : FAILURE_CACHE_TTL);
            return result;
        } catch (Exception exception) {
            log.debug("IP location lookup failed for {}: {}", normalizedIp, exception.getMessage());
            IpLocationResponse unavailable = IpLocationResponse.unknown();
            writeCache(normalizedIp, unavailable, FAILURE_CACHE_TTL);
            return unavailable;
        }
    }

    private URI buildUri(String ipAddress) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl).pathSegment(ipAddress);
        if (apiToken != null && !apiToken.isBlank()) {
            builder.queryParam("token", apiToken.trim());
        }
        return builder.build().encode().toUri();
    }

    private IpLocationResponse parse(String responseBody) throws Exception {
        if (responseBody == null || responseBody.isBlank()) return IpLocationResponse.unknown();
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.path("bogon").asBoolean(false)) return IpLocationResponse.internal();

        String countryCode = text(root, "country");
        String country = localizeCountry(countryCode);
        String region = text(root, "region");
        String city = text(root, "city");
        Set<String> parts = new LinkedHashSet<>();
        addPart(parts, country);
        addPart(parts, region);
        addPart(parts, city);
        if (parts.isEmpty()) return IpLocationResponse.unknown();

        return new IpLocationResponse(country, region, city, String.join(" · ", parts), true);
    }

    private IpLocationResponse readCache(String ipAddress) {
        try {
            String cached = redisTemplate.opsForValue().get(CACHE_PREFIX + ipAddress);
            return cached == null ? null : objectMapper.readValue(cached, IpLocationResponse.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private void writeCache(String ipAddress, IpLocationResponse response, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(
                    CACHE_PREFIX + ipAddress,
                    objectMapper.writeValueAsString(response),
                    ttl
            );
        } catch (Exception ignored) {
            // 지역 캐시 실패는 원래 요청의 성공 여부에 영향을 주지 않는다.
        }
    }

    private String normalize(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank() || "UNKNOWN".equalsIgnoreCase(ipAddress)) return null;
        String value = ipAddress.trim();
        if (value.startsWith("[") && value.endsWith("]")) value = value.substring(1, value.length() - 1);
        if (!isIpLiteral(value)) return null;
        return value;
    }

    private boolean isIpLiteral(String value) {
        if (value.indexOf(':') >= 0) return value.matches("[0-9a-fA-F:]+");
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String part : parts) {
            if (!part.matches("\\d{1,3}")) return false;
            int number = Integer.parseInt(part);
            if (number > 255) return false;
        }
        return true;
    }

    private boolean isInternal(String ipAddress) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress();
        } catch (Exception exception) {
            return true;
        }
    }

    private String localizeCountry(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) return countryCode;
        String localized = new Locale("", countryCode.toUpperCase(Locale.ROOT)).getDisplayCountry(Locale.KOREAN);
        return localized == null || localized.isBlank() ? countryCode : localized;
    }

    private String text(JsonNode root, String field) {
        String value = root.path(field).asText("").trim();
        return value.isEmpty() ? null : value;
    }

    private void addPart(Set<String> parts, String value) {
        if (value != null && !value.isBlank()) parts.add(value);
    }
}
