package spring.study.chat.dto;

public record AudioCallSignalRequest(
        String callId,
        String roomId,
        AudioCallSignalType type,
        String sdp,
        String candidate,
        String sdpMid,
        Integer sdpMLineIndex,
        String targetEmail
) {
    public AudioCallSignalRequest(String callId, String roomId, AudioCallSignalType type,
                                  String sdp, String candidate, String sdpMid,
                                  Integer sdpMLineIndex) {
        this(callId, roomId, type, sdp, candidate, sdpMid, sdpMLineIndex, null);
    }
}
