package spring.study.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;

class EmitterRepositoryTest {

    @Test
    void findAllEmitterStartWithByIdShouldMatchExactMemberPrefix() {
        EmitterRepository emitterRepository = new EmitterRepository();
        SseEmitter memberOneEmitter = new SseEmitter();
        SseEmitter memberTenEmitter = new SseEmitter();

        emitterRepository.save("1_1000", memberOneEmitter);
        emitterRepository.save("10_1000", memberTenEmitter);

        assertThat(emitterRepository.findAllEmitterStartWithById("1"))
                .containsOnlyKeys("1_1000")
                .containsValue(memberOneEmitter);
    }
}
