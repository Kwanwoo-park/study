package spring.study.diary.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import spring.study.aws.service.ImageS3Service;
import spring.study.diary.dto.DiaryImageRequestDto;
import spring.study.diary.dto.DiaryRequestDto;
import spring.study.diary.dto.DiaryResponseDto;
import spring.study.diary.entity.DiaryImage;
import spring.study.diary.repository.DiaryImageRepository;
import spring.study.diary.repository.DiaryRepository;
import spring.study.diary.service.DiaryImageService;
import spring.study.diary.service.DiaryService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
@Import({DiaryFacade.class, DiaryService.class, DiaryImageService.class})
class DiaryImagePersistenceTest {
    @Autowired private DiaryFacade facade;
    @Autowired private DiaryRepository diaryRepository;
    @Autowired private DiaryImageRepository images;
    @Autowired private TestEntityManager em;
    @MockBean private ImageS3Service s3;
    private Member member;

    @BeforeEach
    void setUp() {
        member = em.persistAndFlush(Member.builder().email("diary@example.test").pwd("unused").name("writer")
                .role(Role.USER).phone("01012345678").birth("2000-01-01").profile("profile.png").build());
    }

    @Test
    void omittedImagesArePreservedWhileAnEmptyListDeletesAllImages() {
        DiaryResponseDto created = create("keep.png");
        em.flush();
        Long imageId = images.findAll().get(0).getId();
        DiaryRequestDto unchanged = request(created.getId());
        unchanged.setImages(null);

        facade.update(unchanged, member);
        em.flush();
        assertThat(images.findAll()).extracting(DiaryImage::getId).containsExactly(imageId);

        facade.update(request(created.getId()), member);
        em.flush();
        em.clear();
        assertThat(images.count()).isZero();
        assertThat(diaryRepository.existsById(created.getId())).isTrue();
    }

    @Test
    void diaryDeletionRemovesImagesFirstAndDoesNotViolateTheForeignKey() {
        DiaryResponseDto created = create("remove.png");

        facade.delete(created.getId(), member);
        em.flush();
        em.clear();

        assertThat(diaryRepository.existsById(created.getId())).isFalse();
        assertThat(images.count()).isZero();
    }

    private DiaryResponseDto create(String... urls) {
        Map<?, ?> body = (Map<?, ?>) facade.create(request(null, urls), member).getBody();
        return (DiaryResponseDto) body.get("diary");
    }

    private DiaryRequestDto request(Long id, String... urls) {
        return DiaryRequestDto.builder().id(id).title("제목").content("본문")
                .images(java.util.Arrays.stream(urls).map(url -> DiaryImageRequestDto.builder().imageUrl(url).build()).toList())
                .build();
    }
}
