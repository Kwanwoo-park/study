package spring.study.member.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.member.entity.Member;
import spring.study.member.event.MemberChangedEvent;
import spring.study.member.repository.MemberRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberAudioCallPreferenceServiceTest {

    @Test
    void updateShouldPersistPreferenceAndRefreshAuthenticatedMemberCache() {
        MemberRepository repository = mock(MemberRepository.class);
        VisibilityAccessPolicy visibilityAccessPolicy = mock(VisibilityAccessPolicy.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        MemberService service = new MemberService(repository, visibilityAccessPolicy, eventPublisher);
        Member member = Member.builder().id(7L).email("member@example.com").build();
        when(repository.findById(7L)).thenReturn(Optional.of(member));

        long memberId = service.updateAudioCallEnabled(7L, false);

        assertThat(memberId).isEqualTo(7L);
        assertThat(member.isAudioCallEnabled()).isFalse();
        verify(eventPublisher).publishEvent(any(MemberChangedEvent.class));
    }
}
