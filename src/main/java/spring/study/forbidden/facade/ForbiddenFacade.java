package spring.study.forbidden.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.forbidden.dto.ForbiddenChangeRequestDto;
import spring.study.forbidden.dto.ForbiddenRequestDto;
import spring.study.forbidden.dto.ForbiddenResponseDto;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForbiddenFacade {
    private final ForbiddenService forbiddenService;

    public ResponseEntity<?> search(String word) {
        if (word == null || word.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "검색할 단어가 입력되지 않았습니다"
            ));
        }

        List<ForbiddenResponseDto> list = forbiddenService.findByWord(word);

        return ResponseEntity.ok(Map.of(
                "result", list.size(),
                "list", list
        ));
    }

    public ResponseEntity<?> getStatus(Status status) {
        List<ForbiddenResponseDto> list = forbiddenService.findByStatus(status);

        return ResponseEntity.ok(Map.of(
                "result", list.size(),
                "list", list
        ));
    }

    public ResponseEntity<?> updateStatus(Status status, ForbiddenChangeRequestDto dto) {
        return ResponseEntity.ok(Map.of(
                "result", forbiddenService.updateStatus(status, dto.getIdList())
        ));
    }

    public ResponseEntity<?> wordApply(ForbiddenRequestDto dto) {
        if (dto.getWord() == null || dto.getWord().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "신청할 단어가 없습니다"
            ));
        }

        if (forbiddenService.existWord(dto.getWord())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -1L,
                    "message", "이미 신청된 단어입니다"
            ));
        }

        dto.setStatus(Status.PROPOSAL);

        return ResponseEntity.ok(Map.of(
                "result", forbiddenService.save(dto).getId()
        ));
    }

    public ResponseEntity<?> wordSave(ForbiddenRequestDto dto) {
        if (dto.getWord() == null || dto.getWord().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "승인할 단어가 없습니다"
            ));
        }

        if (forbiddenService.existWord(dto.getWord())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -1L,
                    "message", "이미 승인된 단어입니다"
            ));
        }

        dto.setStatus(Status.APPROVAL);

        return ResponseEntity.ok(Map.of(
                "result", forbiddenService.save(dto).getId()
        ));
    }
}
