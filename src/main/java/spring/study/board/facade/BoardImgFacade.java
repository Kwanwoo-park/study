package spring.study.board.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spring.study.aws.service.ImageS3Service;
import spring.study.aws.service.ImageCleanupService;
import spring.study.board.entity.Board;
import spring.study.board.entity.BoardImg;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardImgFacade {
    private final BoardService boardService;
    private final BoardImgService boardImgService;
    private final ImageS3Service imageS3Service;
    private final ImageCleanupService imageCleanupService;

    public ResponseEntity<Map<String, Object>> imageSave(List<MultipartFile> files, Long id, Member member) {
        int check = imageS3Service.fileSizeCheck(files);

        if (check == -1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
               "result", -99,
               "message", "이미지 파일이 없습니다"
            ));
        } else if (check == -2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -99,
                    "message", "최대 이미지 갯수를 초과하였습니다"
            ));
        }

        if (imageS3Service.findFormatCheck(files)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -99,
                    "message", "지원하지 않는 파일 형식"
            ));
        }

        Board board = boardService.findById(id);
        if (!board.getMember().getId().equals(member.getId()) && member.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -403,
                    "message", "본인 게시글에만 이미지를 추가할 수 있습니다"
            ));
        }

        List<String> uploadedUrls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                String imageUrl = imageS3Service.uploadImageToS3(file);
                uploadedUrls.add(imageUrl);
            }
            boardImgService.saveAll(uploadedUrls.stream()
                    .map(imageUrl -> BoardImg.builder()
                                .imgSrc(imageUrl)
                                .board(board)
                                .build())
                    .toList());

            return ResponseEntity.ok(Map.of(
                    "result", files.size()
            ));
        } catch (Exception e) {
            imageCleanupService.enqueueAll(uploadedUrls);
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "result", -1,
                    "message", "오류가 발생하였습니다"
            ));
        }
    }
}
