package spring.study.controller.board;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.entity.board.Board;
import spring.study.entity.board.BoardImg;
import spring.study.service.board.BoardImgService;
import spring.study.service.board.BoardService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/boardImg")
public class BoardImgApiController {
    private final BoardService boardService;
    private final BoardImgService boardImgService;

    @Value("${img.path}")
    String fileDir;

    @PostMapping("/save")
    public ResponseEntity<List<BoardImg>> boardImgSave(@RequestParam Long id, @RequestPart List<MultipartFile> file, HttpServletRequest request) throws IOException, FileNotFoundException {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        String format;
        String[] formatArr = {"jpg", "jpeg", "png", "gif", "tif", "tiff"};

        Board board = boardService.findById(id);

        List<BoardImg> list = new ArrayList<>();

        File[] files = new File[file.size()];
        try {
            for (int i = 0; i < file.size(); i++) {
                format = StringUtils.getFilenameExtension(file.get(i).getOriginalFilename());

                if (!Arrays.stream(formatArr).toList().contains(format))
                    return ResponseEntity.status(500).body(null);

                files[i] = new File(fileDir + file.get(i).getOriginalFilename());

                if (!files[i].exists()) {
                    file.get(i).transferTo(files[i]);

                    if (!files[i].exists()) {
                        return ResponseEntity.status(500).body(null);
                    }
                }

                list.add(boardImgService.save(BoardImg.builder()
                        .imgSrc(file.get(i).getOriginalFilename())
                        .board(board)
                        .build()));
            }
        }
        catch (FileNotFoundException e) {
            log.debug(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }

        return ResponseEntity.ok(list);
    }
}
