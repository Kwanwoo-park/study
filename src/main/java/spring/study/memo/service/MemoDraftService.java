package spring.study.memo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.memo.config.MemoAutosaveProperties;
import spring.study.memo.dto.MemoDraft;
import spring.study.memo.repository.MemoDraftRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;

@Service
@RequiredArgsConstructor
public class MemoDraftService {
    private final MemoDraftRepository draftRepository;
    private final MemoAutosaveProperties properties;

    public Optional<MemoDraft> find(Long id) {
        return redis(() -> draftRepository.find(id));
    }

    public String lock(Long id) {
        String token = UUID.randomUUID().toString();

        if (!redis(() -> draftRepository.lock(id, token, properties.getLockMs())))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "메모를 저장하고 있습니다. 잠시 후 다시 시도해 주세요");

        return token;
    }

    public void unlock(Long id, String token) {
        draftRepository.unlock(id, token);
    }

    public void requireDurability() {
        if (properties.isRequireDurableRedis())
            redis(() -> {
                draftRepository.requireDurability();

                return null;
            });
    }

    public void store(MemoDraft draft, String token) {
        redis(() -> {
            draftRepository.store(draft, token, dueAt(draft), properties.getCleanTtlSeconds());

            return null;
        });
    }

    public long dueAt(MemoDraft draft) {
        return Math.max(draft.retryAt(), Math.min(draft.changedAt() + properties.getIdleMs(), draft.dirtySince() + properties.getMaxDirtyMs()));
    }

    public void remove(Long id, String token) {
        redis(() -> {
            draftRepository.remove(id, token);

            return null;
        });
    }

    public List<Long> due() {
        return redis(() -> draftRepository.due(System.currentTimeMillis(), properties.getBatchSize()));
    }

    public MemoDraft retry(MemoDraft draft) {
        long delay = Math.min(properties.getRetryMaxMs(), 1000L << Math.min(draft.attempts(), 16));

        return new MemoDraft(draft.memo(), draft.ownerId(), draft.persistedVersion(), draft.dirtySince(), draft.changedAt(),
                System.currentTimeMillis() + delay, draft.attempts() + 1, draft.deleted(), draft.conflicted(), draft.operationKey());
    }

    private <T> T redis(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (DataAccessException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "일시적으로 메모를 불러오거나 저장하지 못했어요. 작성 중인 내용은 복사해 보관한 뒤 잠시 후 다시 시도해 주세요", exception);
        }
    }
}
