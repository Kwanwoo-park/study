package spring.study.common.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.common.entity.IPEntity;

@Getter
@Setter
@NoArgsConstructor
public class IPEntityRequestDto {
    private Long memberId;
    private String ip;

    @Builder
    public IPEntityRequestDto(Long id, Long memberId, String ip) {
        this.memberId = memberId;
        this.ip = ip;
    }

    public IPEntity toEntity() {
        return IPEntity.builder()
                .memberId(memberId)
                .ip(ip)
                .build();
    }

    public void changeIp(String ip) {
        this.ip = ip;
    }
}
