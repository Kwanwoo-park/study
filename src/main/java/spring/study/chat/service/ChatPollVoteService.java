package spring.study.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.chat.dto.ChatPollVoteCount;
import spring.study.chat.entity.ChatPoll;
import spring.study.chat.entity.ChatPollVote;
import spring.study.chat.repository.ChatPollVoteRepository;
import spring.study.member.entity.Member;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatPollVoteService {
    private final ChatPollVoteRepository votes;

    @Transactional(readOnly = true)
    public List<ChatPollVoteCount> counts(List<Long> pollIds) {
        return votes.counts(pollIds);
    }

    @Transactional(readOnly = true)
    public List<ChatPollVote> choices(Member member, List<Long> pollIds) {
        return votes.findByMemberAndPollIdIn(member, pollIds);
    }

    @Transactional
    public void vote(ChatPoll poll, Member member, int optionIndex) {
        votes.findByMemberAndPoll(member, poll).ifPresentOrElse(
                vote -> vote.changeChoice(optionIndex),
                () -> votes.save(new ChatPollVote(poll, member, optionIndex)));
    }
}
