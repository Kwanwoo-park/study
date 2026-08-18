package spring.study.chat.repository;

import spring.study.chat.domain.AudioCall;
import spring.study.chat.domain.AudioCallState;

import java.time.Duration;
import java.util.Optional;

public interface AudioCallStateStore {
    boolean create(AudioCall call, Duration ttl);

    Optional<AudioCall> find(String callId);

    Optional<AudioCall> findByMember(String memberEmail);

    boolean transition(
            AudioCall call,
            AudioCallState expectedState,
            AudioCallState nextState,
            String receiverSessionId,
            Duration ttl
    );

    boolean touch(AudioCall call, AudioCallState expectedState, Duration ttl);

    boolean remove(AudioCall call);
}
