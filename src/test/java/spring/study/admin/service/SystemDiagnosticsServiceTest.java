package spring.study.admin.service;

import org.junit.jupiter.api.Test;
import spring.study.admin.dto.SystemDiskRoot;

import static org.assertj.core.api.Assertions.assertThat;

class SystemDiagnosticsServiceTest {
    @Test
    void filesystemRootIsListedAndCanBeSelected() {
        var disk = new SystemDiagnosticsService().disk("filesystem", "", 0);

        assertThat(disk.roots()).contains(new SystemDiskRoot("filesystem", "파일시스템 루트 (/)"));
        assertThat(disk.root()).isEqualTo("filesystem");
        assertThat(disk.path()).isEmpty();
        assertThat(disk.entries()).isNotEmpty().hasSizeLessThanOrEqualTo(disk.pageSize());
    }
}
