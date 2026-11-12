package spring.study.memo.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;
import spring.study.memo.dto.AdminMemoRequestDto;
import spring.study.memo.dto.AdminMemoResponseDto;
import spring.study.memo.service.AdminMemoService;
import spring.study.common.exception.BusinessStateException;
import spring.study.member.entity.Member;

import java.net.URI;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminMemoFacade {
    private final AdminMemoService memoService;

    public ResponseEntity<?> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("headerName", token.getHeaderName(), "token", token.getToken()));
    }

    public ResponseEntity<?> list(Member member, String query, boolean pinnedOnly, int page) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(memoService.list(member, query, pinnedOnly, page));
    }

    public ResponseEntity<?> find(Long id, Member member) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(memoService.find(id, member));
    }

    public ResponseEntity<?> create(AdminMemoRequestDto request, Member member) {
        AdminMemoResponseDto memo = memoService.create(request, member);
        return ResponseEntity.created(URI.create("/api/admin/memos/" + memo.id())).cacheControl(CacheControl.noStore()).body(memo);
    }

    public ResponseEntity<?> update(Long id, AdminMemoRequestDto request, Member member) {
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(memoService.update(id, request, member));
        } catch (OptimisticLockingFailureException exception) {
            throw conflictingEdit();
        }
    }

    public ResponseEntity<?> delete(Long id, Long version, Member member) {
        try {
            memoService.delete(id, version, member);
            return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        } catch (OptimisticLockingFailureException exception) {
            throw conflictingEdit();
        }
    }

    private BusinessStateException conflictingEdit() {
        return new BusinessStateException("다른 화면에서 변경된 메모입니다. 작성한 내용을 복사한 뒤 메모를 다시 열어 주세요");
    }
}
