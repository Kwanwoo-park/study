package spring.study.chat.dto;

public record ChatPollVoteCount(Long pollId, int optionIndex, long count) {
}
