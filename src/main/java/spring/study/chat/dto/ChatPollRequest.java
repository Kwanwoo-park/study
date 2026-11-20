package spring.study.chat.dto;

import java.util.List;

public record ChatPollRequest(String title, List<String> options, int durationHours) {
}
