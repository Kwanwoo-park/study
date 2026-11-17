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
import spring.study.memo.dto.MemoPageDto;
import spring.study.memo.dto.MemoRequestDto;
import spring.study.memo.dto.MemoResponseDto;
import spring.study.memo.dto.MemoSummaryDto;
import spring.study.memo.dto.MemoDraft;
import spring.study.memo.entity.Memo;
import spring.study.memo.repository.MemoRepository;
import spring.study.common.exception.BusinessStateException;
import spring.study.common.exception.ResourceNotFoundException;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoService {
    private final MemoRepository memoRepository;

    public MemoPageDto list(Member member, String query, boolean pinnedOnly, int page) {
        requireMember(member);

        String keyword = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);

        if (keyword.length() > 200)
            throw new IllegalArgumentException("검색어는 200자 이내로 입력해 주세요");

        String pattern = "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 18, Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("updateTime"), Sort.Order.desc("id")));
        Page<MemoSummaryDto> result = memoRepository.search(member, keyword, pattern, pinnedOnly, pageable);

        return new MemoPageDto(result.getContent(), result.getNumber(), result.getTotalPages(), result.getTotalElements(), memoRepository.countByOwner(member), memoRepository.countByOwnerAndPinnedTrue(member));
    }

    public MemoResponseDto find(Long id, Member member) {
        return new MemoResponseDto(findOwned(id, member));
    }

    @Transactional
    public MemoResponseDto create(MemoRequestDto request, Member member) {
        requireMember(member);
        validate(request);

        String memoTitle = title(request);

        if (request.creationKey() != null)
            UUID.fromString(request.creationKey());

        return new MemoResponseDto(memoRepository.saveAndFlush(Memo.builder()
                .owner(member)
                .creationKey(request.creationKey())
                .title(memoTitle.isBlank() ? "제목 없는 메모" : memoTitle)
                .content(content(request))
                .color(request.color())
                .pinned(request.pinned())
                .build()));
    }

    @Transactional
    public void deleteByMember(Member member) {
        memoRepository.deleteByOwner(member);
    }

    @Transactional(timeout = 10)
    public void deleteStored(Long id, long ownerId) {
        memoRepository.deleteByIdAndOwner_Id(id, ownerId);
    }

    public Optional<MemoResponseDto> findCreated(String creationKey, Member member) {
        requireMember(member);

        if (creationKey == null)
            return Optional.empty();

        UUID.fromString(creationKey);

        return memoRepository.findByCreationKeyAndOwner(creationKey, member).map(MemoResponseDto::new);
    }

    @Transactional(timeout = 10)
    public boolean persist(MemoDraft draft) {
        MemoResponseDto memo = draft.memo();
        int changed = memoRepository.persistDraft(memo.id(), draft.ownerId(), draft.persistedVersion(), memo.version(), memo.title(), memo.content(), memo.color(), memo.pinned(), memo.updatedAt());

        if (changed == 1)
            return true;

        Optional<Memo> current = memoRepository.findById(memo.id());

        if (current.isEmpty() || current.get().getOwner().getId() != draft.ownerId())
            return false;

        Memo saved = current.get();
        // A DB commit can succeed even when acknowledging it in Redis fails.
        if (saved.getVersion() == memo.version() && saved.getTitle().equals(memo.title()) && saved.getContent().equals(memo.content()) && saved.getColor() == memo.color() && saved.isPinned() == memo.pinned())
            return true;

        throw new BusinessStateException("다른 화면에서 변경된 메모입니다. 내용을 복사한 뒤 저장본을 다시 열어 주세요");
    }

    private Memo findOwned(Long id, Member member) {
        requireMember(member);

        if (id == null || id < 1)
            throw new ResourceNotFoundException("메모를 찾을 수 없습니다");

        return memoRepository.findByIdAndOwner(id, member).orElseThrow(() -> new ResourceNotFoundException("메모를 찾을 수 없습니다"));
    }

    private void requireMember(Member member) {
        if (member == null || member.getId() == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");

        if (member.getRole() != Role.ADMIN && member.getRole() != Role.USER)
            throw new AccessDeniedException("메모를 사용할 수 없는 계정입니다");
    }

    private void validate(MemoRequestDto request) {
        if (request == null || title(request).isBlank() && content(request).isBlank())
            throw new IllegalArgumentException("제목이나 메모 내용을 입력해 주세요");
        else if (request.title() != null && request.title().length() > 120)
            throw new IllegalArgumentException("제목은 120자 이내로 입력해 주세요");
        else if (content(request).length() > 20000)
            throw new IllegalArgumentException("메모는 20,000자 이내로 입력해 주세요");
        else if (request.color() == null)
            throw new IllegalArgumentException("메모 색상을 선택해 주세요");
    }

    private String title(MemoRequestDto request) {
        return request.title() == null ? "" : request.title().strip();
    }

    private String content(MemoRequestDto request) {
        return request.content() == null ? "" : request.content();
    }

}
