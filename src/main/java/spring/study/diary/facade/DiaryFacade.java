package spring.study.diary.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import spring.study.aws.service.ImageS3Service;
import spring.study.diary.dto.DiaryImageRequestDto;
import spring.study.diary.dto.DiaryRequestDto;
import spring.study.diary.dto.DiaryResponseDto;
import spring.study.diary.dto.DiaryTodoRequestDto;
import spring.study.diary.entity.Diary;
import spring.study.diary.entity.DiaryImage;
import spring.study.diary.service.DiaryService;
import spring.study.member.entity.Member;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiaryFacade {
    private final DiaryService diaryService;
    private final ImageS3Service imageS3Service;

    public ResponseEntity<?> uploadImages(List<MultipartFile> files) {
        int fileCount = imageS3Service.fileSizeCheck(files);
        if (fileCount == -1) {
            throw new IllegalArgumentException("이미지 파일이 없습니다");
        }
        if (fileCount == -2) {
            throw new IllegalArgumentException("이미지는 최대 10장까지 업로드할 수 있습니다");
        }
        if (imageS3Service.findFormatCheck(files)) {
            throw new IllegalArgumentException("JPG, JPEG, PNG, GIF 이미지만 업로드할 수 있습니다");
        }

        List<String> uploadedUrls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                uploadedUrls.add(imageS3Service.uploadImageToS3(file));
            }
            return ResponseEntity.ok(Map.of(
                    "result", uploadedUrls.size(),
                    "imageUrls", uploadedUrls
            ));
        } catch (IOException exception) {
            imageS3Service.deleteImgSrc(uploadedUrls);
            log.error("일기 이미지 업로드 실패", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "result", -1,
                    "message", "이미지 업로드 중 오류가 발생했습니다"
            ));
        }
    }

    @Transactional
    public ResponseEntity<?> create(DiaryRequestDto requestDto, Member member) {
        validate(member, requestDto, false);
        DiaryResponseDto diary = diaryService.save(requestDto, member);
        return ResponseEntity.ok(Map.of(
                "result", diary.getId(),
                "diary", diary
        ));
    }

    @Transactional(readOnly = true)
    public DiaryResponseDto findById(Long id, Member member) {
        return new DiaryResponseDto(getOwnedDiary(id, member));
    }

    @Transactional(readOnly = true)
    public List<DiaryResponseDto> findByMember(Member member, int page, int size) {
        if (member == null) {
            throw new AccessDeniedException("로그인이 필요합니다");
        }
        return diaryService.findResponseByMember(member, page, size);
    }

    @Transactional(readOnly = true)
    public long count(Member member) {
        if (member == null) {
            throw new AccessDeniedException("로그인이 필요합니다");
        }
        return diaryService.countByMember(member);
    }

    @Transactional
    public ResponseEntity<?> update(DiaryRequestDto requestDto, Member member) {
        validate(member, requestDto, true);
        Diary diary = getOwnedDiary(requestDto.getId(), member);
        List<String> removedImageUrls = new ArrayList<>();
        diary.update(requestDto.getTitle(), requestDto.getContent());
        diary.changeUpdateTime(LocalDateTime.now());

        if (requestDto.getImages() != null) {
            Set<String> requestedImageUrls = requestDto.getImages().stream()
                    .filter(java.util.Objects::nonNull)
                    .map(DiaryImageRequestDto::getImageUrl)
                    .collect(Collectors.toSet());
            removedImageUrls = diary.getImages().stream()
                    .map(DiaryImage::getImageUrl)
                    .filter(url -> !requestedImageUrls.contains(url))
                    .toList();

            new ArrayList<>(diary.getImages()).forEach(diary::removeImage);
            requestDto.getImages().stream()
                    .filter(java.util.Objects::nonNull)
                    .map(DiaryImageRequestDto::toEntity)
                    .forEach(diary::addImage);
        }

        if (requestDto.getTodos() != null) {
            new ArrayList<>(diary.getTodos()).forEach(diary::removeTodo);
            requestDto.getTodos().stream()
                    .filter(java.util.Objects::nonNull)
                    .map(DiaryTodoRequestDto::toEntity)
                    .forEach(diary::addTodo);
        }

        DiaryResponseDto responseDto = new DiaryResponseDto(diaryService.save(diary));
        if (!removedImageUrls.isEmpty()) {
            imageS3Service.deleteImgSrc(removedImageUrls);
        }
        return ResponseEntity.ok(Map.of(
                "result", responseDto.getId(),
                "diary", responseDto
        ));
    }

    @Transactional
    public ResponseEntity<?> delete(Long id, Member member) {
        Diary diary = getOwnedDiary(id, member);
        List<String> imageUrls = diary.getImages().stream()
                .map(DiaryImage::getImageUrl)
                .toList();
        diaryService.delete(diary);
        if (!imageUrls.isEmpty()) {
            imageS3Service.deleteImgSrc(imageUrls);
        }
        return ResponseEntity.ok(Map.of(
                "result", id
        ));
    }

    private Diary getOwnedDiary(Long id, Member member) {
        if (member == null || member.getId() == null) {
            throw new AccessDeniedException("로그인이 필요합니다");
        }
        return diaryService.findByIdAndMember(id, member);
    }

    private void validate(Member member, DiaryRequestDto requestDto, boolean requireId) {
        if (member == null) {
            throw new AccessDeniedException("로그인이 필요합니다");
        }
        if (requestDto == null) {
            throw new IllegalArgumentException("일기 내용을 입력해 주세요");
        }
        if (requireId && requestDto.getId() == null) {
            throw new IllegalArgumentException("일기 ID가 필요합니다");
        }
        if (requestDto.getTitle() == null || requestDto.getTitle().isBlank()) {
            throw new IllegalArgumentException("제목을 입력해 주세요");
        }
        if (requestDto.getTitle().length() > 200) {
            throw new IllegalArgumentException("제목은 200자 이하여야 합니다");
        }
        if (requestDto.getContent() == null) {
            throw new IllegalArgumentException("본문을 입력해 주세요");
        }
    }
}
