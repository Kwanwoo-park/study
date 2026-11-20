package spring.study.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.chat.dto.ChatPollVoteCount;
import spring.study.chat.entity.ChatPoll;
import spring.study.chat.entity.ChatPollVote;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Optional;

public interface ChatPollVoteRepository extends JpaRepository<ChatPollVote, Long> {
    @Query("""
            select new spring.study.chat.dto.ChatPollVoteCount(v.poll.id, v.optionIndex, count(v))
            from ChatPollVote v where v.poll.id in :pollIds
            group by v.poll.id, v.optionIndex
            """)
    List<ChatPollVoteCount> counts(@Param("pollIds") List<Long> pollIds);

    List<ChatPollVote> findByMemberAndPollIdIn(Member member, List<Long> pollIds);

    Optional<ChatPollVote> findByMemberAndPoll(Member member, ChatPoll poll);
}
