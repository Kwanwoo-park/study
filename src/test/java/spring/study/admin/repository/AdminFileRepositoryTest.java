package spring.study.admin.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import spring.study.admin.entity.AdminFile;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
class AdminFileRepositoryTest {
    @Autowired AdminFileRepository repository;
    @Autowired TestEntityManager entityManager;

    @Test
    void persistsMetadataAndPaginatesNewestFirst() {
        for (int index = 0; index < 22; index++) {
            AdminFile file = new AdminFile(UUID.randomUUID().toString(), "한글 파일 " + index + ".exe", 1024, 7L);
            ReflectionTestUtils.setField(file, "createdAt", LocalDateTime.of(2026, 9, 5, 12, 0).plusSeconds(index));
            repository.saveAndFlush(file);
        }
        var first = repository.findAll(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        assertThat(first.getTotalElements()).isEqualTo(22);
        assertThat(first.getContent()).hasSize(20);
        assertThat(first.getContent().get(0).getOriginalFilename()).isEqualTo("한글 파일 21.exe");
        assertThat(first.getContent().get(0).getUploadedBy()).isEqualTo(7L);
        assertThat(repository.findById(first.getContent().get(0).getId())).isPresent();
    }

    @Test
    void persistsOnlyFileMetadata() {
        String id = UUID.randomUUID().toString();
        repository.saveAndFlush(new AdminFile(id, "test.exe", 4, 7L));
        entityManager.clear();
        AdminFile stored = repository.findById(id).orElseThrow();
        assertThat(stored.getOriginalFilename()).isEqualTo("test.exe");
        assertThat(stored.getSize()).isEqualTo(4);
        assertThat(stored.getUploadedBy()).isEqualTo(7L);
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(entityManager.getEntityManager().getMetamodel().entity(AdminFile.class).getAttributes())
                .extracting(attribute -> attribute.getName())
                .containsExactlyInAnyOrder("id", "originalFilename", "size", "uploadedBy", "createdAt");
    }
}
