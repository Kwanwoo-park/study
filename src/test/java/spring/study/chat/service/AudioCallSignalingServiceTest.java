package spring.study.chat.service;

import org.junit.jupiter.api.Test;
import spring.study.chat.domain.AudioCall;
import spring.study.chat.domain.AudioCallParticipantStatus;
import spring.study.chat.domain.AudioCallState;
import spring.study.chat.repository.AudioCallStateStore;
import spring.study.common.exception.BusinessStateException;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AudioCallSignalingServiceTest {
    private final AudioCallStateStore store = mock(AudioCallStateStore.class);
    private final AudioCallSignalingService service = new AudioCallSignalingService(store);

    @Test
    void stateStoreMustBeTheOnlyInjectedDependency() {
        var constructors = AudioCallSignalingService.class.getDeclaredConstructors();
        assertThat(constructors).hasSize(1);
        assertThat(constructors[0].getParameterTypes()).containsExactly(AudioCallStateStore.class);
        assertThat(Arrays.stream(AudioCallSignalingService.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(field -> field.getType().getName()))
                .containsExactly(AudioCallStateStore.class.getName());
    }

    @Test
    void startingCallShouldKeepTheRingingTtl() {
        AudioCall call = ringingCall();
        when(store.create(call, Duration.ofSeconds(45))).thenReturn(true);

        service.startCall(call);

        verify(store).create(call, Duration.ofSeconds(45));
    }

    @Test
    void acceptingCallShouldReturnTheAtomicStoreResultWithActiveTtl() {
        AudioCall call = ringingCall();
        AudioCall updated = call.withParticipant("receiver@test.com", "receiver-session", AudioCallParticipantStatus.JOINED);
        when(store.join(call, "receiver@test.com", "receiver-session", Duration.ofHours(12)))
                .thenReturn(Optional.of(updated));

        assertSame(updated, service.accept(call, "receiver@test.com", "receiver-session"));
    }

    @Test
    void concurrentAcceptFailureMustNotBeTreatedAsSuccess() {
        AudioCall call = ringingCall();
        when(store.join(call, "receiver@test.com", "receiver-session", Duration.ofHours(12)))
                .thenReturn(Optional.empty());

        assertThrows(BusinessStateException.class,
                () -> service.accept(call, "receiver@test.com", "receiver-session"));
    }

    @Test
    void anotherSessionMustNotChangeOrExtendTheCall() {
        AudioCall call = ringingCall();

        assertThrows(BusinessStateException.class,
                () -> service.keepAlive(call, "caller@test.com", "another-session"));
        assertThrows(BusinessStateException.class,
                () -> service.leave(call, "caller@test.com", "another-session"));

        verifyNoInteractions(store);
    }

    private AudioCall ringingCall() {
        return new AudioCall("call-1", "room-1",
                "caller@test.com", "발신자", "caller-session",
                "receiver@test.com", "수신자", null, AudioCallState.RINGING);
    }
}
