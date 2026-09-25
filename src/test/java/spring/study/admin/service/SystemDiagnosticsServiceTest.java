package spring.study.admin.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemDiagnosticsServiceTest {
    private final SystemDiagnosticsService service = new SystemDiagnosticsService();

    @Test
    void parsesProcessMemoryWithoutExposingCommandArguments() {
        var row = SystemDiagnosticsService.parseProcessLine("  123  2048 /usr/bin/java");
        assertEquals(123L, row.pid());
        assertEquals("java", row.name());
        assertEquals(2L * 1024 * 1024, row.memoryBytes());
    }

    @Test
    void ignoresMalformedProcessRows() {
        assertEquals(null, SystemDiagnosticsService.parseProcessLine("not a process"));
        assertEquals(null, SystemDiagnosticsService.parseProcessLine("12 0"));
    }

    @Test
    void diskListingIsPagedAndLimitedToAllowedRoots() {
        var snapshot = service.disk("project", "", 0);
        assertEquals("project", snapshot.root());
        assertTrue(snapshot.entries().size() <= 100);
        assertTrue(snapshot.roots().stream().anyMatch(root -> root.id().equals("project")));
        assertFalse(snapshot.entries().stream().anyMatch(row -> row.name().equals("..")));
        assertThrows(IllegalArgumentException.class, () -> service.disk("unknown", "", 0));
        assertThrows(IllegalArgumentException.class, () -> service.disk("project", "../../..", 0));
        assertThrows(IllegalArgumentException.class, () -> service.disk("project", "/etc", 0));
        assertThrows(IllegalArgumentException.class, () -> service.disk("project", "", -1));
    }
}
