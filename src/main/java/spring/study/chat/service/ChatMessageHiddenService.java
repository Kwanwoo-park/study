package spring.study.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageHidden;
import spring.study.chat.repository.ChatMessageHiddenRepository;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatMessageHiddenService {
    private final ChatMessageHiddenRepository chatMessageHiddenRepository;

    @Transactional
    public ChatMessage hideForMember(ChatMessage message, Member member) {
        if (!chatMessageHiddenRepository.existsByMessageAndMember(message, member)) {
            chatMessageHiddenRepository.save(ChatMessageHidden.builder()
                    .message(message)
                    .member(member)
                    .hiddenAt(LocalDateTime.now())
                    .build());
        }

        return message;
    }
}
