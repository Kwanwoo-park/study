package spring.study.diary.facade;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import spring.study.aws.service.ImageS3Service;
import spring.study.diary.dto.DiaryImageRequestDto;
import spring.study.diary.dto.DiaryListResponseDto;
import spring.study.diary.dto.DiaryRequestDto;
import spring.study.diary.dto.DiaryResponseDto;
import spring.study.diary.entity.Diary;
import spring.study.diary.entity.DiaryImage;
import spring.study.diary.service.DiaryImageService;
import spring.study.diary.service.DiaryService;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DiaryFacadeTest {
    private final DiaryService service = mock(DiaryService.class);
    private final DiaryImageService imageService = mock(DiaryImageService.class);
    private final ImageS3Service s3 = mock(ImageS3Service.class);
    private final DiaryFacade facade = new DiaryFacade(service, imageService, s3);
    private final Member member = Member.builder().id(1L).build();

    @Test
    void detailApiWrapsTheOwnedDiary() {
        Diary diary = Diary.builder().id(12L).member(member).title("제목").content("본문").build();
        when(service.findByIdAndMember(12L, member)).thenReturn(diary);

        Map<?, ?> body = (Map<?, ?>) facade.findById(12L, member).getBody();
        assertThat(body.get("result")).isEqualTo(12L);
        DiaryResponseDto dto = (DiaryResponseDto) body.get("diary");
        assertThat(dto.getId()).isEqualTo(12L);
        assertThat(dto.getContent()).isEqualTo("본문");
        verify(service).findByIdAndMember(12L, member);
    }

    @Test
    void webDetailStillReturnsADiaryDto() {
        Diary diary = Diary.builder().id(12L).member(member).title("제목").content("본문").build();
        when(service.findByIdAndMember(12L, member)).thenReturn(diary);

        DiaryResponseDto dto = facade.findDetail(12L, member);
        assertThat(dto.getId()).isEqualTo(12L);
        assertThat(dto.getMemberId()).isEqualTo(member.getId());
        verify(service).findByIdAndMember(12L, member);
    }

    @Test
    void apiAndWebDetailKeepOwnershipChecks() {
        when(service.findByIdAndMember(12L, member)).thenThrow(new AccessDeniedException("Forbidden"));
        assertThatThrownBy(() -> facade.findById(12L, member)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> facade.findDetail(12L, member)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listKeepsPaginationResponse() {
        List<DiaryListResponseDto> diaries = List.of(new DiaryListResponseDto());
        when(service.findListByMember(member, 0, 1)).thenReturn(diaries);
        when(service.countByMember(member)).thenReturn(2L);

        assertThat(facade.load(member, 0, 1).getBody()).isEqualTo(Map.of(
                "result", 1, "diaries", diaries, "totalCount", 2L, "hasNext", true, "nextPage", 1));
    }

    @Test
    void searchUsesFilteredCountAndStopsAtTheLastPage() {
        List<DiaryListResponseDto> diaries = List.of(new DiaryListResponseDto());
        when(service.searchByTitle(member, "검색", 1, 1)).thenReturn(diaries);
        when(service.countByMemberAndTitle(member, "검색")).thenReturn(2L);

        assertThat(facade.search(member, "검색", 1, 1).getBody()).isEqualTo(Map.of(
                "result", 1, "diaries", diaries, "totalCount", 2L, "hasNext", false, "nextPage", 0));
        verify(service, never()).countByMember(member);
    }

    @Test
    void blankSearchStillUsesTheNormalList() {
        when(service.findListByMember(member, 0, 10)).thenReturn(List.of());

        assertThat(facade.search(member, " ", 0, 10).getBody()).isEqualTo(Map.of(
                "result", 0, "diaries", List.of(), "totalCount", 0L, "hasNext", false, "nextPage", 0));
        verify(service).findListByMember(member, 0, 10);
    }

    @Test
    void createExplicitlySavesImagesAfterSavingTheDiary() {
        Diary diary = Diary.builder().id(12L).member(member).title("제목").content("본문").build();
        DiaryRequestDto request = request(List.of(image("new.png")));
        when(service.create(request, member)).thenReturn(diary);

        Map<?, ?> body = (Map<?, ?>) facade.create(request, member).getBody();

        var order = inOrder(service, imageService);
        order.verify(service).create(request, member);
        order.verify(imageService).saveAll(argThat(images -> images.size() == 1
                && images.get(0).getDiary() == diary && images.get(0).getImageUrl().equals("new.png")));
        assertThat(((DiaryResponseDto) body.get("diary")).getImages()).hasSize(1);
    }

    @Test
    void updateExplicitlyReplacesImageRowsButOnlyDeletesRemovedS3Files() {
        Diary diary = Diary.builder().id(12L).member(member).title("제목").content("본문").build();
        diary.addImage(DiaryImage.builder().id(1L).imageUrl("keep.png").build());
        diary.addImage(DiaryImage.builder().id(2L).imageUrl("remove.png").build());
        when(service.findByIdAndMember(12L, member)).thenReturn(diary);
        when(service.save(diary)).thenAnswer(invocation -> new DiaryResponseDto(diary));

        facade.update(request(List.of(image("keep.png"), image("new.png"))), member);

        var order = inOrder(imageService, service);
        order.verify(service).findByIdAndMember(12L, member);
        order.verify(imageService).deleteByDiary(diary);
        order.verify(imageService).saveAll(argThat(images -> images.size() == 2));
        order.verify(service).save(diary);
        assertThat(diary.getImages()).extracting(DiaryImage::getImageUrl).containsExactly("keep.png", "new.png");
        verify(s3).deleteImgSrc(List.of("remove.png"));
    }

    @Test
    void omittedImagesDoNotCallTheImageService() {
        Diary diary = Diary.builder().id(12L).member(member).title("제목").content("본문").build();
        diary.addImage(DiaryImage.builder().id(1L).imageUrl("keep.png").build());
        when(service.findByIdAndMember(12L, member)).thenReturn(diary);
        when(service.save(diary)).thenReturn(new DiaryResponseDto(diary));

        facade.update(request(null), member);

        verifyNoInteractions(imageService);
        assertThat(diary.getImages()).hasSize(1);
    }

    @Test
    void deleteRemovesImageRowsBeforeDeletingTheDiary() {
        Diary diary = Diary.builder().id(12L).member(member).title("제목").content("본문").build();
        diary.addImage(DiaryImage.builder().id(1L).imageUrl("remove.png").build());
        when(service.findByIdAndMember(12L, member)).thenReturn(diary);

        facade.delete(12L, member);

        var order = inOrder(imageService, service);
        order.verify(service).findByIdAndMember(12L, member);
        order.verify(imageService).deleteByDiary(diary);
        order.verify(service).delete(diary);
        verify(s3).deleteImgSrc(List.of("remove.png"));
    }

    private DiaryRequestDto request(List<DiaryImageRequestDto> images) {
        return DiaryRequestDto.builder().id(12L).title("제목").content("본문").images(images).build();
    }

    private DiaryImageRequestDto image(String url) {
        return DiaryImageRequestDto.builder().imageUrl(url).build();
    }
}
