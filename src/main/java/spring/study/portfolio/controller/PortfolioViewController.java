package spring.study.portfolio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PortfolioViewController {

    @GetMapping("/portfolio")
    public String redirectPortfolio() {
        return "redirect:/portfolio/";
    }

    @GetMapping("/portfolio/")
    public String portfolio() {
        return "forward:/portfolio/index.html";
    }
}
