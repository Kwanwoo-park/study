package spring.study.jwt.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import spring.study.member.entity.Member;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;

    @Value("${security.jwt.secret}")
    private String configuredSecret;

    @Value("${security.jwt.issuer}")
    private String issuer;

    @Value("${security.jwt.access-token-minutes}")
    private long accessTokenMinutes;

    @Value("${security.jwt.refresh-token-days}")
    private long refreshTokenDays;

    private byte[] secret;

    @PostConstruct
    void initializeSecret() {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException("security.jwt.secret must be configured");
        }
        secret = configuredSecret.getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("security.jwt.secret must contain at least 32 bytes");
        }
    }

    public IssuedToken createAccessToken(Member member) {
        return createToken(member, ACCESS, accessTokenDuration());
    }

    public IssuedToken createRefreshToken(Member member) {
        return createToken(member, REFRESH, refreshTokenDuration());
    }

    public TokenClaims parse(String token, String expectedType) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new JwtValidationException("Malformed JWT");

            byte[] expectedSignature = sign(parts[0] + "." + parts[1]);
            byte[] actualSignature = BASE64_URL_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw new JwtValidationException("Invalid JWT signature");
            }

            Map<String, Object> header = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(parts[0]), new TypeReference<>() {});
            if (!"HS256".equals(header.get("alg"))) throw new JwtValidationException("Unsupported JWT algorithm");

            Map<String, Object> payload = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(parts[1]), new TypeReference<>() {});
            String type = stringClaim(payload, "type");
            if (!expectedType.equals(type)) throw new JwtValidationException("Unexpected JWT type");
            if (!issuer.equals(stringClaim(payload, "iss"))) throw new JwtValidationException("Invalid JWT issuer");

            Instant expiresAt = Instant.ofEpochSecond(longClaim(payload, "exp"));
            if (!expiresAt.isAfter(Instant.now())) throw new JwtValidationException("JWT expired");

            return new TokenClaims(
                    longClaim(payload, "uid"),
                    stringClaim(payload, "sub"),
                    stringClaim(payload, "role"),
                    type,
                    stringClaim(payload, "jti"),
                    expiresAt
            );
        } catch (JwtValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JwtValidationException("Invalid JWT", exception);
        }
    }

    public String encodeAuthorizationRequest(OAuth2AuthorizationRequest request) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ObjectOutputStream objectOutput = new ObjectOutputStream(output)) {
                objectOutput.writeObject(request);
            }
            String payload = BASE64_URL_ENCODER.encodeToString(output.toByteArray());
            return payload + "." + BASE64_URL_ENCODER.encodeToString(sign(payload));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not encode OAuth2 authorization request", exception);
        }
    }

    public OAuth2AuthorizationRequest decodeAuthorizationRequest(String value) {
        try {
            String[] parts = value.split("\\.");
            if (parts.length != 2 || !MessageDigest.isEqual(sign(parts[0]), BASE64_URL_DECODER.decode(parts[1]))) {
                return null;
            }
            try (ObjectInputStream input = new ObjectInputStream(
                    new ByteArrayInputStream(BASE64_URL_DECODER.decode(parts[0])))) {
                Object request = input.readObject();
                return request instanceof OAuth2AuthorizationRequest authorizationRequest
                        ? authorizationRequest : null;
            }
        } catch (Exception exception) {
            return null;
        }
    }

    public Duration accessTokenDuration() {
        return Duration.ofMinutes(accessTokenMinutes);
    }

    public Duration refreshTokenDuration() {
        return Duration.ofDays(refreshTokenDays);
    }

    private IssuedToken createToken(Member member, String type, Duration duration) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(duration);
        String jti = UUID.randomUUID().toString();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", issuer);
        payload.put("sub", member.getEmail());
        payload.put("uid", member.getId());
        payload.put("role", member.getRole().getValue());
        payload.put("type", type);
        payload.put("jti", jti);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        try {
            String headerPart = BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(header));
            String payloadPart = BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            String unsigned = headerPart + "." + payloadPart;
            String token = unsigned + "." + BASE64_URL_ENCODER.encodeToString(sign(unsigned));
            return new IssuedToken(token, jti, expiresAt);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create JWT", exception);
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign JWT", exception);
        }
    }

    private String stringClaim(Map<String, Object> payload, String name) {
        Object value = payload.get(name);
        if (!(value instanceof String string) || string.isBlank()) {
            throw new JwtValidationException("Missing JWT claim: " + name);
        }
        return string;
    }

    private long longClaim(Map<String, Object> payload, String name) {
        Object value = payload.get(name);
        if (!(value instanceof Number number)) throw new JwtValidationException("Missing JWT claim: " + name);
        return number.longValue();
    }

    public record IssuedToken(String value, String jti, Instant expiresAt) {}
    public record TokenClaims(Long memberId, String email, String role, String type, String jti, Instant expiresAt) {}

    public static class JwtValidationException extends RuntimeException {
        public JwtValidationException(String message) { super(message); }
        public JwtValidationException(String message, Throwable cause) { super(message, cause); }
    }
}
