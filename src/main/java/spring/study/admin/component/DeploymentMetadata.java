package spring.study.admin.component;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import spring.study.admin.dto.DeploymentInfo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Properties;

@Component
public class DeploymentMetadata {
    private final DeploymentInfo snapshot = read();

    public DeploymentInfo snapshot() {
        return snapshot;
    }

    private DeploymentInfo read() {
        LocalDateTime start = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()), ZoneId.systemDefault());
        ClassPathResource metadata = new ClassPathResource("META-INF/study-build.properties");
        if (!metadata.exists()) return new DeploymentInfo("", false, start);

        try (InputStream content = metadata.getInputStream()) {
            Properties properties = new Properties();
            properties.load(content);
            return new DeploymentInfo(properties.getProperty("commit", ""),
                    Boolean.parseBoolean(properties.getProperty("dirty")), start);
        } catch (IOException error) {
            return new DeploymentInfo("", false, start);
        }
    }
}
