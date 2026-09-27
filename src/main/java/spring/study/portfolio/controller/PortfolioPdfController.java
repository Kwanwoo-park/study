package spring.study.portfolio.controller;

import spring.study.portfolio.facade.PortfolioPdfFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PortfolioPdfController {
    private final PortfolioPdfFacade facade;

    @GetMapping("/api/portfolio/pdf")
    public ResponseEntity<Resource> download() {
        return facade.download();
    }
}
