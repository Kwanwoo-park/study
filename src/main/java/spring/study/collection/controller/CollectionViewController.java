package spring.study.collection.controller;

import spring.study.collection.facade.CollectionViewFacade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@Controller
@RequestMapping("/collection")
@Slf4j
public class CollectionViewController {
    private final CollectionViewFacade facade;

    @GetMapping("")
    public String collectionMain(Model model, HttpServletRequest request) {
        return facade.collectionMain(model, request);
    }
}
