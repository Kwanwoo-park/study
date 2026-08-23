package spring.study.appeal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AppealRequestDto {
    private String email;
    private String password;
    private Long sanctionId;

    @NotBlank(message = "상소문 제목을 입력해주세요")
    @Size(max = 100, message = "상소문 제목은 100자 이하여야 합니다")
    private String title;

    @NotBlank(message = "상소 내용을 입력해주세요")
    @Size(max = 4000, message = "상소 내용은 4000자 이하여야 합니다")
    private String content;
}
