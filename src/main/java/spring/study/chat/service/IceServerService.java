package spring.study.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import spring.study.chat.dto.IceServerDto;
import spring.study.member.entity.Member;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class IceServerService {
    private final String stunUrl;
    private final List<String> turnUrls;
    private final String turnSharedSecret;
    private final long credentialTtlSeconds;

    public IceServerService(@Value("${webrtc.ice.stun-url}") String stunUrl, @Value("${webrtc.ice.turn-urls}") List<String> turnUrls, @Value("${webrtc.ice.turn-shared-secret}") String turnSharedSecret, @Value("${webrtc.ice.turn-credential-ttl-seconds}") long credentialTtlSeconds) {
        this.stunUrl = stunUrl;
        this.turnUrls = turnUrls.stream().filter(url -> url != null && !url.isBlank()).toList();
        this.turnSharedSecret = turnSharedSecret;
        this.credentialTtlSeconds = credentialTtlSeconds;
    }

    public List<IceServerDto> createIceServers(Member member) {
        List<IceServerDto> servers = new ArrayList<>();
        if (stunUrl != null && !stunUrl.isBlank()) servers.add(IceServerDto.stun(stunUrl));

        if (!turnUrls.isEmpty() && turnSharedSecret != null && !turnSharedSecret.isBlank()) {
            long expiresAt = Instant.now().getEpochSecond() + credentialTtlSeconds;
            String username = expiresAt + ":" + member.getId();
            servers.add(IceServerDto.turn(turnUrls, username, createCredential(username)));
        }
        return servers;
    }

    private String createCredential(String username) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA1");
            hmac.init(new SecretKeySpec(turnSharedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(
                    hmac.doFinal(username.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("TURN 자격증명을 생성할 수 없습니다.", exception);
        }
    }
}
