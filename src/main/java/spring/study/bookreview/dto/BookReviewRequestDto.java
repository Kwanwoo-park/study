package spring.study.bookreview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class BookReviewRequestDto {
    @NotBlank(message = "독후감 제목을 입력해주세요")
    @Size(max = 200, message = "독후감 제목은 200자 이하여야 합니다")
    private String reviewTitle;

    @NotBlank(message = "책 제목을 입력해주세요")
    @Size(max = 200, message = "책 제목은 200자 이하여야 합니다")
    private String bookTitle;

    @NotBlank(message = "책 저자를 입력해주세요")
    @Size(max = 100, message = "책 저자는 100자 이하여야 합니다")
    private String bookAuthor;

    @NotNull(message = "별점을 선택해주세요")
    @Min(value = 1, message = "별점은 1점 이상이어야 합니다")
    @Max(value = 5, message = "별점은 5점 이하여야 합니다")
    private Integer rating;

    private LocalDate finishedDate;

    @NotBlank(message = "독후감 내용을 입력해주세요")
    @Size(max = 20000, message = "독후감 내용은 20,000자 이하여야 합니다")
    private String content;
}
