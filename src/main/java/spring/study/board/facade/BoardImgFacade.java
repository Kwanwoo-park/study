package spring.study.board.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spring.study.aws.service.ImageS3Service;
import spring.study.board.entity.Board;
import spring.study.board.entity.BoardImg;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardImgFacade {
    private final BoardService boardService;
    private final BoardImgService boardImgService;
    private final ImageS3Service imageS3Service;

    public ResponseEntity<Map<String, Object>> imageSave(List<MultipartFile> files, Long id) {
        if (files.size() > 10) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -2,
                    "message", "업로드 파일 갯수 초과"
            ));
        }

        if (imageS3Service.findFormatCheck(files)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -99,
                    "message", "지원하지 않는 파일 형식"
            ));
        }

        Board board = boardService.findById(id);

        try {
            for (MultipartFile file : files) {
                String imageUrl = imageS3Service.uploadImageToS3(file);

                boardImgService.save(BoardImg.builder()
                                .imgSrc(imageUrl)
                                .board(board)
                        .build());
            }

            return ResponseEntity.ok(Map.of(
                    "result", files.size()
            ));
        } catch (IOException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "result", -1,
                    "message", "오류가 발생하였습니다"
            ));
        }
    }
}
