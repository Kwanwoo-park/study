package spring.study.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatPollResponse(Long id, String title, List<String> options, List<Long> counts, Integer myChoice,
                               LocalDateTime endsAt, boolean closed) {
}
