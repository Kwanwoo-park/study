package spring.study.common.config;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.flywaydb.core.api.MigrationVersion;

@Configuration
public class DatabaseMigrationConfig {
    @Bean
    FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> configuration
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"));
    }
}
