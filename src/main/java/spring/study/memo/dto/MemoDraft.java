package spring.study.memo.dto;

public record MemoDraft(MemoResponseDto memo, long ownerId, long persistedVersion,
                        long dirtySince, long changedAt, long retryAt, int attempts,
                        boolean deleted, boolean conflicted, String operationKey) {
    public static MemoDraft persisted(MemoResponseDto memo, long ownerId) {
        return new MemoDraft(memo, ownerId, memo.version(), 0, 0, 0, 0, false, false, null);
    }

    public boolean pending() {
        return deleted || memo.version() != persistedVersion;
    }

    public MemoResponseDto response() {
        return new MemoResponseDto(memo.id(), memo.title(), memo.content(), memo.color(), memo.pinned(),
                memo.version(), memo.createdAt(), memo.updatedAt(), pending(), conflicted);
    }
}
