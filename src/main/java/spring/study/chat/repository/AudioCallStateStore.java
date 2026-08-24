package spring.study.chat.repository;

import spring.study.chat.domain.AudioCall;
import spring.study.chat.domain.AudioCallMutation;

import java.time.Duration;
import java.util.Optional;

public interface AudioCallStateStore {
    boolean create(AudioCall call, Duration ttl);

    Optional<AudioCall> find(String callId);

    Optional<AudioCall> findByMember(String memberEmail);

    Optional<AudioCall> join(AudioCall call, String memberEmail, String sessionId, Duration ttl);

    Optional<AudioCallMutation> reject(AudioCall call, String memberEmail, Duration ttl);

    Optional<AudioCallMutation> leave(
            AudioCall call, String memberEmail, String sessionId, Duration ttl);

    boolean touch(AudioCall call, String memberEmail, String sessionId, Duration ttl);

    boolean remove(AudioCall call);
}
