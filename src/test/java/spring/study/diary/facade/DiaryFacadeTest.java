package spring.study.diary.facade;

import org.junit.jupiter.api.Test;
import spring.study.aws.service.ImageS3Service;
import spring.study.aws.service.ImageCleanupService;
import spring.study.diary.dto.DiaryListResponseDto;
import spring.study.diary.service.DiaryService;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DiaryFacadeTest {
    private final DiaryService service = mock(DiaryService.class);
    private final DiaryFacade facade = new DiaryFacade(service, mock(ImageS3Service.class), mock(ImageCleanupService.class));
    private final Member member = Member.builder().id(1L).build();

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
}
