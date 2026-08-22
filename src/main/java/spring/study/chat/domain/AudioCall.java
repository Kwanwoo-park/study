package spring.study.chat.domain;

public record AudioCall(String callId, String roomId, String callerEmail, String callerName, String callerSessionId, String receiverEmail, String receiverName, String receiverSessionId, AudioCallState state) {
    public boolean contains(String email) {
        return callerEmail.equals(email) || receiverEmail.equals(email);
    }

    public boolean isCaller(String email) {
        return callerEmail.equals(email);
    }

    public boolean isReceiver(String email) {
        return receiverEmail.equals(email);
    }

    public String otherEmail(String email) {
        if (isCaller(email)) return receiverEmail;
        if (isReceiver(email)) return callerEmail;
        throw new IllegalArgumentException("통화 참여자가 아닙니다.");
    }

    public String otherSessionId(String email) {
        if (isCaller(email)) return receiverSessionId;
        if (isReceiver(email)) return callerSessionId;
        throw new IllegalArgumentException("통화 참여자가 아닙니다.");
    }

    public String nameOf(String email) {
        if (isCaller(email)) return callerName;
        if (isReceiver(email)) return receiverName;
        throw new IllegalArgumentException("통화 참여자가 아닙니다.");
    }

    public boolean ownsSession(String email, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return false;
        if (isCaller(email)) return sessionId.equals(callerSessionId);
        if (isReceiver(email)) return sessionId.equals(receiverSessionId);
        return false;
    }
}
