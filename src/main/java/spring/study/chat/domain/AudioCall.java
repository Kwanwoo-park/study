package spring.study.chat.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record AudioCall(
        String callId,
        String roomId,
        String initiatorEmail,
        String initiatorName,
        List<AudioCallParticipant> participants,
        AudioCallState state
) {
    public AudioCall {
        participants = participants == null ? List.of() : List.copyOf(participants);
    }

    public AudioCall(String callId, String roomId,
                     String callerEmail, String callerName, String callerSessionId,
                     String receiverEmail, String receiverName, String receiverSessionId,
                     AudioCallState state) {
        this(callId, roomId, callerEmail, callerName, List.of(
                new AudioCallParticipant(
                        callerEmail, callerName, callerSessionId,
                        AudioCallParticipantStatus.JOINED),
                new AudioCallParticipant(
                        receiverEmail, receiverName, receiverSessionId,
                        receiverSessionId == null
                                ? AudioCallParticipantStatus.INVITED
                                : AudioCallParticipantStatus.JOINED)
        ), state);
    }

    public boolean contains(String email) {
        return participant(email).filter(AudioCallParticipant::isAvailable).isPresent();
    }

    public boolean isCaller(String email) {
        return initiatorEmail.equals(email);
    }

    public boolean isReceiver(String email) {
        return !isCaller(email) && participant(email).isPresent();
    }

    public String otherEmail(String email) {
        if (isCaller(email)) return receiverEmail();
        if (isReceiver(email)) return initiatorEmail;
        throw new IllegalArgumentException("통화 참여자가 아닙니다.");
    }

    public String otherSessionId(String email) {
        if (isCaller(email)) return receiverSessionId();
        if (isReceiver(email)) return callerSessionId();
        throw new IllegalArgumentException("통화 참여자가 아닙니다.");
    }

    public String nameOf(String email) {
        return participant(email)
                .map(AudioCallParticipant::name)
                .orElseThrow(() -> new IllegalArgumentException("통화 참여자가 아닙니다."));
    }

    public boolean ownsSession(String email, String sessionId) {
        return participant(email).filter(participant -> participant.ownsSession(sessionId)).isPresent();
    }

    public Optional<AudioCallParticipant> participant(String email) {
        if (email == null) return Optional.empty();
        return participants.stream().filter(participant -> email.equals(participant.email())).findFirst();
    }

    public List<AudioCallParticipant> joinedParticipants() {
        return participants.stream()
                .filter(participant -> participant.status() == AudioCallParticipantStatus.JOINED)
                .toList();
    }

    public List<AudioCallParticipant> invitedParticipants() {
        return participants.stream()
                .filter(participant -> participant.status() == AudioCallParticipantStatus.INVITED)
                .toList();
    }

    public AudioCall withParticipant(String email, String sessionId, AudioCallParticipantStatus status) {
        List<AudioCallParticipant> updated = new ArrayList<>(participants.size());
        for (AudioCallParticipant participant : participants) {
            updated.add(email.equals(participant.email())
                    ? new AudioCallParticipant(participant.email(), participant.name(), sessionId, status)
                    : participant);
        }
        AudioCallState nextState = status == AudioCallParticipantStatus.JOINED
                ? AudioCallState.ACTIVE
                : state;
        return new AudioCall(callId, roomId, initiatorEmail, initiatorName, updated, nextState);
    }

    public String callerEmail() {
        return initiatorEmail;
    }

    public String callerName() {
        return initiatorName;
    }

    public String callerSessionId() {
        return participant(initiatorEmail).map(AudioCallParticipant::sessionId).orElse(null);
    }

    public String receiverEmail() {
        return participants.stream()
                .filter(participant -> !initiatorEmail.equals(participant.email()))
                .map(AudioCallParticipant::email)
                .findFirst()
                .orElse(null);
    }

    public String receiverName() {
        return participant(receiverEmail()).map(AudioCallParticipant::name).orElse(null);
    }

    public String receiverSessionId() {
        return participant(receiverEmail()).map(AudioCallParticipant::sessionId).orElse(null);
    }
}
