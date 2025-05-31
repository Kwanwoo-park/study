package spring.study.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.board.entity.Board;
import spring.study.board.entity.BoardImg;
import spring.study.aws.service.ImageS3Service;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/boardImg")
public class BoardImgApiController {
    private final BoardService boardService;
    private final BoardImgService boardImgService;
    private final ImageS3Service imageS3Service;

    @PostMapping("/save")
    public ResponseEntity<List<BoardImg>> boardImgSave(@RequestParam Long id, @RequestPart List<MultipartFile> file, HttpServletRequest request) throws IOException, FileNotFoundException {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        if (imageS3Service.findFormatCheck(file))
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);

        Board board = boardService.findById(id);

        List<BoardImg> list = new ArrayList<>();

        for (MultipartFile multipartFile : file) {
            String imageUrl = imageS3Service.uploadImageToS3(multipartFile);

            list.add(boardImgService.save(BoardImg.builder()
                    .imgSrc(imageUrl)
                    .board(board)
                    .build()));
        }

        return ResponseEntity.ok(list);
    }
}
