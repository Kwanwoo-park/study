package spring.study.member.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import spring.study.jwt.service.MemberTokenCacheService;

@Component
@RequiredArgsConstructor
public class MemberCacheEventListener {
    private final MemberTokenCacheService memberTokenCacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void refreshMemberCache(MemberChangedEvent event) {
        memberTokenCacheService.refreshIfPresent(event.memberId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void deleteMemberCache(MemberDeletedEvent event) {
        memberTokenCacheService.delete(event.memberId());
    }
}
