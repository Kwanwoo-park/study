package spring.study.memo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import spring.study.memo.dto.AdminMemoPageDto;
import spring.study.memo.dto.AdminMemoRequestDto;
import spring.study.memo.dto.AdminMemoResponseDto;
import spring.study.memo.dto.AdminMemoSummaryDto;
import spring.study.memo.entity.AdminMemo;
import spring.study.memo.repository.AdminMemoRepository;
import spring.study.common.exception.BusinessStateException;
import spring.study.common.exception.ResourceNotFoundException;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemoService {
    private final AdminMemoRepository memoRepository;

    public AdminMemoPageDto list(Member administrator, String query, boolean pinnedOnly, int page) {
        requireAdministrator(administrator);
        String keyword = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (keyword.length() > 200) throw new IllegalArgumentException("검색어는 200자 이내로 입력해 주세요");
        String pattern = "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 18,
                Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("updateTime"), Sort.Order.desc("id")));
        Page<AdminMemoSummaryDto> result = memoRepository.search(administrator, keyword, pattern, pinnedOnly, pageable);
        return new AdminMemoPageDto(result.getContent(), result.getNumber(), result.getTotalPages(), result.getTotalElements(),
                memoRepository.countByOwner(administrator), memoRepository.countByOwnerAndPinnedTrue(administrator));
    }

    public AdminMemoResponseDto find(Long id, Member administrator) {
        return new AdminMemoResponseDto(findOwned(id, administrator));
    }

    @Transactional
    public AdminMemoResponseDto create(AdminMemoRequestDto request, Member administrator) {
        requireAdministrator(administrator);
        validate(request);
        String memoTitle = title(request);
        AdminMemo memo = AdminMemo.builder().owner(administrator).title(memoTitle.isBlank() ? "제목 없는 메모" : memoTitle).content(content(request))
                .color(request.color()).pinned(request.pinned()).build();
        return new AdminMemoResponseDto(memoRepository.saveAndFlush(memo));
    }

    @Transactional
    public AdminMemoResponseDto update(Long id, AdminMemoRequestDto request, Member administrator) {
        requireAdministrator(administrator);
        validate(request);
        AdminMemo memo = findOwned(id, administrator);
        requireVersion(memo, request.version());
        String memoTitle = title(request);
        memo.revise(memoTitle.isBlank() ? "제목 없는 메모" : memoTitle, content(request), request.color(), request.pinned());
        memoRepository.flush();
        return new AdminMemoResponseDto(memo);
    }

    @Transactional
    public void delete(Long id, Long version, Member administrator) {
        AdminMemo memo = findOwned(id, administrator);
        requireVersion(memo, version);
        memoRepository.delete(memo);
        memoRepository.flush();
    }

    @Transactional
    public void deleteByMember(Member member) {
        memoRepository.deleteByOwner(member);
    }

    private AdminMemo findOwned(Long id, Member administrator) {
        requireAdministrator(administrator);
        if (id == null || id < 1) throw new ResourceNotFoundException("메모를 찾을 수 없습니다");
        return memoRepository.findByIdAndOwner(id, administrator)
                .orElseThrow(() -> new ResourceNotFoundException("메모를 찾을 수 없습니다"));
    }

    private void requireAdministrator(Member member) {
        if (member == null || member.getId() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        if (member.getRole() != Role.ADMIN) throw new AccessDeniedException("관리자만 메모를 사용할 수 있습니다");
    }

    private void validate(AdminMemoRequestDto request) {
        if (request == null || title(request).isBlank() && content(request).isBlank()) {
            throw new IllegalArgumentException("제목이나 메모 내용을 입력해 주세요");
        }
        if (request.title() != null && request.title().length() > 120) throw new IllegalArgumentException("제목은 120자 이내로 입력해 주세요");
        if (content(request).length() > 20000) throw new IllegalArgumentException("메모는 20,000자 이내로 입력해 주세요");
        if (request.color() == null) throw new IllegalArgumentException("메모 색상을 선택해 주세요");
    }

    private String title(AdminMemoRequestDto request) {
        return request.title() == null || request.title().isBlank() ? "" : request.title().strip();
    }

    private String content(AdminMemoRequestDto request) {
        return request.content() == null ? "" : request.content();
    }

    private void requireVersion(AdminMemo memo, Long version) {
        if (version == null || version < 0) throw new IllegalArgumentException("메모 버전 정보가 없습니다. 목록에서 다시 열어 주세요");
        if (memo.getVersion() != version) throw new BusinessStateException("다른 화면에서 변경된 메모입니다. 작성한 내용을 복사한 뒤 메모를 다시 열어 주세요");
    }
}
