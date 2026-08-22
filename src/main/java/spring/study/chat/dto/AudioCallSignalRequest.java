package spring.study.chat.dto;

public record AudioCallSignalRequest(String callId, String roomId, AudioCallSignalType type, String sdp, String candidate, String sdpMid, Integer sdpMLineIndex) {
}
