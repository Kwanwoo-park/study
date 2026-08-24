package spring.study.chat.domain;

public record AudioCallParticipant(
        String email,
        String name,
        String sessionId,
        AudioCallParticipantStatus status
) {
    public boolean ownsSession(String candidateSessionId) {
        return candidateSessionId != null && !candidateSessionId.isBlank()
                && candidateSessionId.equals(sessionId);
    }

    public boolean isAvailable() {
        return status == AudioCallParticipantStatus.INVITED
                || status == AudioCallParticipantStatus.JOINED;
    }
}
