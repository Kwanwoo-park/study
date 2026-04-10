package spring.study.collection.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spring.study.aws.service.ImageS3Service;
import spring.study.collection.dto.CollectionRequestDto;
import spring.study.collection.dto.CollectionResponseDto;
import spring.study.collection.entity.Collection;
import spring.study.collection.service.CollectionService;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionFacade {
    private final CollectionService collectionService;
    private final ImageS3Service imageS3Service;

    public ResponseEntity<?> load(int cursor, int limit, Member member) {
        List<CollectionResponseDto> list = collectionService.getCollections(cursor, limit, member);
        int nextCursor = list.isEmpty() ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "collection", list,
                "nextCursor", nextCursor,
                "result", 10L
        ));
    }

    public ResponseEntity<?> save(HttpServletRequest request, CollectionRequestDto dto, Member member) {
        Collection collection = dto.toEntity();
        collection.addMember(member);

        request.getSession(false).setAttribute("member", member);

        Collection saved = collectionService.save(collection);

        if (saved == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "result", -10,
                    "message", "저장되지 않았습니다"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "result", saved.getId()
        ));
    }

    public ResponseEntity<?> saveImage(MultipartFile file) {
        if (imageS3Service.fileFormatCheck(file)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "지원하지 않는 파일 포맷입니다"
            ));
        }

        try {
            String imageUrl = imageS3Service.uploadImageToS3(file);

            return ResponseEntity.ok(Map.of(
                    "result", 10,
                    "imgSrc", imageUrl
            ));
        } catch (Exception e) {
            log.error("profile image change failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "result", -10L,
                    "message", "이미지 업로드 중 오류가 발생하였습니다"
            ));
        }
    }

    public ResponseEntity<?> delete(Long id, Member member) {
        Collection collection = collectionService.findById(id);

        if (!collection.getMember().getId().equals(member.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -1,
                    "message", "본인만 지울 수 있습니다"
            ));
        }

        collectionService.deleteById(id);

        return ResponseEntity.ok(Map.of(
                "result", 1L
        ));
    }
}
