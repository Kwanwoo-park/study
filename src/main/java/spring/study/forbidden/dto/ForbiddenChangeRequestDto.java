package spring.study.forbidden.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ForbiddenChangeRequestDto {
    private List<Long> idList;

    @Builder
    public ForbiddenChangeRequestDto(List<Long> idList) {
        this.idList = idList;
    }
}
