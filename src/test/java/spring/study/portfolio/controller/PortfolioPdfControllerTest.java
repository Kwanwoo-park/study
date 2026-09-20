package spring.study.portfolio.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import spring.study.portfolio.service.PortfolioPdfService;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PortfolioPdfControllerTest {
    @Test
    void downloadsPdfAsAttachmentAndPreservesUnavailableStatus() throws Exception {
        PortfolioPdfService service = mock(PortfolioPdfService.class);
        byte[] pdf = "%PDF-1.7 test".getBytes(StandardCharsets.US_ASCII);
        when(service.download()).thenReturn(pdf).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        var mvc = MockMvcBuilders.standaloneSetup(new PortfolioPdfController(service)).build();
        mvc.perform(get("/api/portfolio/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"study-portfolio.pdf\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(pdf));
        mvc.perform(get("/api/portfolio/pdf")).andExpect(status().isServiceUnavailable());
    }
}
