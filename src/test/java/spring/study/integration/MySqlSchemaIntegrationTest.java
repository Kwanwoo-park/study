package spring.study.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("infrastructure")
class MySqlSchemaIntegrationTest {
    @Test
    void migrationKeepsLegacyRoomAndCanBeAppliedAgain() throws Exception {
        String url = Objects.requireNonNull(System.getenv("STUDY_TEST_MYSQL_URL"), "Set STUDY_TEST_MYSQL_URL to a test MySQL server");
        String user = System.getenv().getOrDefault("STUDY_TEST_MYSQL_USER", "root");
        String password = System.getenv().getOrDefault("STUDY_TEST_MYSQL_PASSWORD", "");
        String schema = "study_integration_" + UUID.randomUUID().toString().replace("-", "");

        try (var connection = DriverManager.getConnection(url, user, password);
             var admin = connection.createStatement()) {
            admin.execute("CREATE DATABASE " + schema);
            connection.setCatalog(schema);
            try (var sql = connection.createStatement()) {
                sql.execute("CREATE TABLE room (id BIGINT PRIMARY KEY, name VARCHAR(255) NOT NULL)");
                sql.execute("INSERT INTO room VALUES (1, 'existing room')");
                var migration = new ClassPathResource("db/mysql/V20261123_01__chat_access_and_image_upload.sql");

                ScriptUtils.executeSqlScript(connection, migration);
                ScriptUtils.executeSqlScript(connection, migration);

                try (var room = sql.executeQuery("SELECT name, direct_room FROM room WHERE id = 1")) {
                    assertThat(room.next()).isTrue();
                    assertThat(room.getString("name")).isEqualTo("existing room");
                    assertThat(room.getObject("direct_room")).isNull();
                }
                sql.execute("INSERT INTO chat_image_upload VALUES ('upload', 7, 1, 'READY', CURRENT_TIMESTAMP(6))");
                try (var history = sql.executeQuery("SELECT version FROM study_schema_migration")) {
                    assertThat(history.next()).isTrue();
                    assertThat(history.getString(1)).isEqualTo("20261123_01");
                    assertThat(history.next()).isFalse();
                }
            } finally {
                try (var cleanup = connection.createStatement()) {
                    cleanup.execute("DROP DATABASE " + schema);
                }
            }
        }
    }
}
