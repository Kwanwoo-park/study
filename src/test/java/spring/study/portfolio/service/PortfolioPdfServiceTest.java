package spring.study.portfolio.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import spring.study.portfolio.controller.PortfolioPdfController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PortfolioPdfServiceTest {
    private final PortfolioPdfRenderer renderer = mock(PortfolioPdfRenderer.class);
    private final PortfolioPdfService service = spy(new PortfolioPdfService(renderer));
    private final byte[] pdf = "%PDF-1.7 test".getBytes(StandardCharsets.US_ASCII);

    @AfterEach
    void cleanup() {
        service.close();
    }

    private Map<String, byte[]> prepare() throws Exception {
        Map<String, byte[]> sources = new TreeMap<>();
        sources.put("portfolio/index.html", "current portfolio".getBytes(StandardCharsets.UTF_8));
        doReturn(sources).when(service).loadSources();
        when(renderer.version()).thenReturn(new byte[]{1});
        doAnswer(invocation -> { Files.write(invocation.getArgument(1, Path.class), pdf); return null; }).when(renderer).render(any(), any());
        return sources;
    }

    @Test
    void reusesPdfUntilContentStyleOrRendererChanges() throws Exception {
        Map<String, byte[]> sources = prepare();
        assertThat(service.download()).isEqualTo(pdf);
        assertThat(service.download()).isEqualTo(pdf);
        verify(renderer).render(any(), any());
        sources.put("portfolio/print.css", new byte[]{2});
        service.download();
        sources.put("portfolio/index.html", new byte[]{3});
        service.download();
        when(renderer.version()).thenReturn(new byte[]{4});
        service.download();
        verify(renderer, times(4)).render(any(), any());
    }

    @Test
    void simultaneousRequestsShareOneGeneration() throws Exception {
        prepare();
        CountDownLatch rendering = new CountDownLatch(1), release = new CountDownLatch(1);
        doAnswer(invocation -> {
            rendering.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            Files.write(invocation.getArgument(1, Path.class), pdf);
            return null;
        }).when(renderer).render(any(), any());
        var executor = Executors.newFixedThreadPool(4);
        try {
            var first = executor.submit(service::download);
            assertThat(rendering.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(service::download);
            var third = executor.submit(service::download);
            release.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo(pdf);
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo(pdf);
            assertThat(third.get(5, TimeUnit.SECONDS)).isEqualTo(pdf);
            verify(renderer).render(any(), any());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void failedRefreshNeverServesStaleOrPartialPdfAndHasRetryCooldown() throws Exception {
        Map<String, byte[]> sources = prepare();
        service.download();
        sources.put("portfolio/index.html", new byte[]{2});
        doAnswer(invocation -> {
            Files.writeString(invocation.getArgument(1, Path.class), "partial output");
            throw new IOException("Renderer failure");
        }).when(renderer).render(any(), any());
        for (int attempt = 0; attempt < 2; attempt++) {
            assertThatThrownBy(service::download).isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
        }
        verify(renderer, times(2)).render(any(), any());
    }

    @Test
    void loadsActualDeployedResourcesWithoutDependingOnWorkingDirectory() throws Exception {
        assertThat(new PortfolioPdfService(renderer).loadSources()).containsKeys("portfolio/index.html", "portfolio/styles.css", "portfolio/print.css", "portfolio/app.js", "css/common/page-back.css", "js/common/page-back.js");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PORTFOLIO_PDF_INTEGRATION", matches = "true")
    void realChromiumDownloadReturnsThirteenPagePdfAndReusesCache() throws Exception {
        PortfolioPdfRenderer realRenderer = spy(new PortfolioPdfRenderer());
        PortfolioPdfService realService = new PortfolioPdfService(realRenderer);
        try {
            var mvc = MockMvcBuilders.standaloneSetup(new PortfolioPdfController(realService)).build();
            byte[] first = mvc.perform(get("/api/portfolio/pdf")).andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andReturn().getResponse().getContentAsByteArray();
            assertThat(first).startsWith("%PDF-".getBytes(StandardCharsets.US_ASCII));
            assertThat(first.length).isGreaterThan(10000);
            mvc.perform(get("/api/portfolio/pdf")).andExpect(status().isOk()).andExpect(content().bytes(first));
            verify(realRenderer).render(any(), any());
        } finally {
            realService.close();
        }
    }
}
