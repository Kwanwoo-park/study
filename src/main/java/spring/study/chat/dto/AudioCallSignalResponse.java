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
        String error
) {
    public static AudioCallSignalResponse from(
            AudioCallSignalRequest request, String senderEmail, String senderName) {
        return new AudioCallSignalResponse(
                request.callId(), request.roomId(), request.type(), senderEmail, senderName,
                request.sdp(), request.candidate(), request.sdpMid(), request.sdpMLineIndex(), null);
    }

    public static AudioCallSignalResponse error(AudioCallSignalRequest request, String message) {
        return new AudioCallSignalResponse(
                request.callId(), request.roomId(), request.type(), null, null,
                null, null, null, null, message);
    }
}
