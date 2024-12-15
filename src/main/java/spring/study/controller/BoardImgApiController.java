package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.service.BoardImgService;
import spring.study.service.BoardService;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/boardImg")
public class BoardImgApiController {
    private final BoardService boardService;
    private final BoardImgService boardImgService;

    @PostMapping("/save")
    public ResponseEntity<Integer> boardImgSave(@RequestParam Long id, @RequestPart List<MultipartFile> file, HttpServletRequest request) throws IOException {
        //String fileDir = "/home/ec2-user/app/step1/study/src/main/resources/static/img/";
        String fileDir = "/Users/lg/Desktop/study/study/src/main/resources/static/img/";

        File[] files = new File[file.size()];

        for (int i = 0; i < file.size(); i++) {
            files[i] = new File(fileDir + file.get(i).getOriginalFilename());

            if (!files[i].exists()) {
                file.get(i).transferTo(files[i]);
            }
        }

        return ResponseEntity.status(200).body(null);
    }
}
