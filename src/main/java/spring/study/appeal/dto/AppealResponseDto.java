package spring.study.appeal.dto;

import lombok.Getter;
import spring.study.appeal.entity.Appeal;
import spring.study.appeal.entity.AppealStatus;

import java.time.LocalDateTime;

@Getter
public class AppealResponseDto {
    private final Long id;
    private final String memberEmail;
    private final String memberName;
    private final Long sanctionId;
    private final Long reportId;
    private final String title;
    private final String content;
    private final AppealStatus status;
    private final LocalDateTime registerTime;

    public AppealResponseDto(Appeal appeal) {
        this.id = appeal.getId();
        this.memberEmail = appeal.getMember().getEmail();
        this.memberName = appeal.getMember().getName();
        this.sanctionId = appeal.getRelatedSanction() == null ? null : appeal.getRelatedSanction().getId();
        this.reportId = appeal.getRelatedSanction() == null
                ? null
                : appeal.getRelatedSanction().getReport().getId();
        this.title = appeal.getTitle();
        this.content = appeal.getContent();
        this.status = appeal.getStatus();
        this.registerTime = appeal.getRegisterTime();
    }
}
