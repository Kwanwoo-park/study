package spring.study.chat.dto;

public record AudioCallSignalResponse(
        String callId,
        String roomId,
        AudioCallSignalType type,
        String senderEmail,
        String senderName,
        String sdp,
        String candidate,
        String sdpMid,
        Integer sdpMLineIndex,
        String targetEmail,
        String error
) {
    public static AudioCallSignalResponse from(AudioCallSignalRequest request, String senderEmail, String senderName) {
        return new AudioCallSignalResponse(
                request.callId(), request.roomId(), request.type(), senderEmail, senderName,
                request.sdp(), request.candidate(), request.sdpMid(), request.sdpMLineIndex(),
                request.targetEmail(), null);
    }

    public static AudioCallSignalResponse error(AudioCallSignalRequest request, String message) {
        if (request == null) {
            return new AudioCallSignalResponse(
                    null, null, null, null, null,
                    null, null, null, null, null, message);
        }
        return new AudioCallSignalResponse(
                request.callId(), request.roomId(), request.type(), null, null,
                null, null, null, null, request.targetEmail(), message);
    }

    public static AudioCallSignalResponse disconnected(String callId, String roomId, String disconnectedMemberEmail) {
        return new AudioCallSignalResponse(
                callId, roomId, AudioCallSignalType.DISCONNECTED,
                disconnectedMemberEmail, null, null, null, null, null, null, null);
    }

    public static AudioCallSignalResponse accepted(String callId, String roomId) {
        return new AudioCallSignalResponse(
                callId, roomId, AudioCallSignalType.ACCEPTED,
                null, null, null, null, null, null, null, null);
    }

    public static AudioCallSignalResponse adminTerminated(String callId, String roomId) {
        return new AudioCallSignalResponse(
                callId, roomId, AudioCallSignalType.ADMIN_TERMINATED,
                null, null, null, null, null, null, null, null);
    }

    public static AudioCallSignalResponse participantEvent(
            String callId,
            String roomId,
            AudioCallSignalType type,
            String participantEmail,
            String participantName
    ) {
        return new AudioCallSignalResponse(
                callId, roomId, type, participantEmail, participantName,
                null, null, null, null, null, null);
    }
}
