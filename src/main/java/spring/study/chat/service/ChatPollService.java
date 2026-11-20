package spring.study.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import spring.study.chat.dto.ChatPollRequest;
import spring.study.chat.entity.ChatPoll;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.repository.ChatPollRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatPollService {
    private final ChatPollRepository polls;

    @Transactional(readOnly = true)
    public List<ChatPoll> list(ChatRoom room) {
        return polls.findByRoomOrderByIdDesc(room, PageRequest.of(0, 20));
    }

    @Transactional
    public ChatPoll create(ChatRoom room, ChatPollRequest request) {
        if (request.title() == null || request.title().isBlank() || request.title().length() > 100
                || request.options() == null || request.options().size() < 2 || request.options().size() > 5
                || !List.of(1, 24, 168).contains(request.durationHours())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문, 선택지 2~5개와 종료 시간을 확인해 주세요.");
        }
        if (request.options().stream().anyMatch(option -> option == null || option.isBlank() || option.length() > 80)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "각 선택지는 1~80자로 입력해 주세요.");
        }
        List<String> options = request.options().stream().map(String::strip).distinct().toList();
        if (options.size() != request.options().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "중복된 선택지가 있습니다.");
        }
        return polls.save(new ChatPoll(room, request.title().strip(), LocalDateTime.now().plusHours(request.durationHours()), options));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ChatPoll lock(Long id, ChatRoom room) {
        return polls.findForUpdateByIdAndRoom(id, room)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다."));
    }
}
