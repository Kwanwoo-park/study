package spring.study.chat.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import spring.study.chat.dto.ChatPollRequest;
import spring.study.chat.dto.ChatPollResponse;
import spring.study.chat.dto.ChatPollVoteCount;
import spring.study.chat.entity.ChatPoll;
import spring.study.chat.entity.ChatPollVote;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.service.ChatPollService;
import spring.study.chat.service.ChatPollVoteService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.common.component.TransactionRunner;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPollFacade {
    private final ChatRoomService rooms;
    private final ChatRoomMemberService members;
    private final ChatPollService polls;
    private final ChatPollVoteService votes;
    private final TransactionRunner transactions;
    private final SimpMessagingTemplate messaging;

    @Transactional(readOnly = true)
    public List<ChatPollResponse> list(String roomId, Member member) {
        List<ChatPoll> list = polls.list(requireParticipant(roomId, member));
        if (list.isEmpty()) return List.of();
        List<Long> ids = list.stream().map(ChatPoll::getId).toList();
        Map<Long, List<Long>> counts = new HashMap<>();
        for (ChatPoll poll : list) counts.put(poll.getId(), new ArrayList<>(Collections.nCopies(poll.getOptions().size(), 0L)));
        for (ChatPollVoteCount count : votes.counts(ids)) counts.get(count.pollId()).set(count.optionIndex(), count.count());
        Map<Long, Integer> choices = new HashMap<>();
        for (ChatPollVote vote : votes.choices(member, ids)) choices.put(vote.getPoll().getId(), vote.getOptionIndex());
        LocalDateTime now = LocalDateTime.now();
        return list.stream().map(poll -> new ChatPollResponse(poll.getId(), poll.getTitle(), List.copyOf(poll.getOptions()),
                counts.get(poll.getId()), choices.get(poll.getId()), poll.getEndsAt(), poll.isClosed(now))).toList();
    }

    public void create(String roomId, ChatPollRequest request, Member member) {
        transactions.write(() -> polls.create(requireParticipant(roomId, member), request));
        broadcast(roomId);
    }

    public void vote(String roomId, Long pollId, Integer optionIndex, Member member) {
        transactions.write(() -> {
            ChatPoll poll = polls.lock(pollId, requireParticipant(roomId, member));
            if (poll.isClosed(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "종료된 투표입니다.");
            }
            if (optionIndex == null || optionIndex < 0 || optionIndex >= poll.getOptions().size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택지를 확인해 주세요.");
            }
            votes.vote(poll, member, optionIndex);
            return null;
        });
        broadcast(roomId);
    }

    private ChatRoom requireParticipant(String roomId, Member member) {
        ChatRoom room = rooms.find(roomId);
        if (room == null || member.isAccessBlocked() || !members.exist(member, room)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "채팅방 참여자만 투표를 이용할 수 있습니다.");
        }
        return room;
    }

    private void broadcast(String roomId) {
        try {
            messaging.convertAndSend("/sub/chat/room/" + roomId, Map.of("action", "POLL_UPDATED"));
        } catch (RuntimeException error) {
            log.warn("Chat poll refresh could not be sent. roomId={}", roomId);
        }
    }
}
