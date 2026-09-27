package spring.study.admin.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.admin.dto.AdminNewBoardResponseDto;
import spring.study.admin.service.SystemIncidentService;
import spring.study.admin.service.AdminSystemStatusService;
import spring.study.admin.service.SystemDiagnosticsService;
import spring.study.appeal.dto.AppealResponseDto;
import spring.study.appeal.entity.AppealStatus;
import spring.study.appeal.service.AppealService;
import spring.study.board.service.BoardService;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.AudioCallSignalingService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.common.service.OnlineUserService;
import spring.study.jwt.service.RefreshTokenService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFacade {
    private final MemberService memberService;
    private final BoardService boardService;
    private final AppealService appealService;
    private final spring.study.appeal.facade.AppealFacade appealFacade;
    private final ChatMessageService chatMessageService;
    private final SystemIncidentService systemIncidentService;
    private final AudioCallSignalingService audioCallSignalingService;
    private final OnlineUserService onlineUserService;
    private final RefreshTokenService refreshTokenService;
    private final AdminSystemStatusService adminSystemStatusService;
    private final SystemDiagnosticsService systemDiagnosticsService;

    public ResponseEntity<?> memberOnline() {
        List<Long> userIds = onlineUserService.findOnlineMemberIds().stream().toList();
        long count = userIds.size();
        onlineUserService.syncOnlineTotal();

        List<Member> list = memberService.findMember(userIds);

        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "count", count,
                "list", list
        ));
    }

    public ResponseEntity<?> newMember() {
        LocalDateTime end = LocalDateTime.now(), start = end.minusDays(1);

        List<Member> list = memberService.findNewUser(start, end);
        int count = list.size();

        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "count", count,
                "list", list
        ));
    }

    public ResponseEntity<?> newBoard() {
        LocalDateTime end = LocalDateTime.now(), start = end.minusDays(7);

        List<AdminNewBoardResponseDto> list = boardService.findNewBoard(start, end).stream()
                .map(AdminNewBoardResponseDto::new)
                .toList();
        int count = list.size();

        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "count", count,
                "list", list
        ));
    }

    public ResponseEntity<?> chattingActive() {
        LocalDateTime end = LocalDateTime.now(), start = end.minusHours(1);

        List<ChatRoom> list = chatMessageService.findActiveChatting(start, end).stream()
                .map(ChatMessage::getRoom)
                .distinct()
                .toList();
        int count = list.size();

        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "count", count,
                "list", list
        ));
    }

    public ResponseEntity<?> systemStatus() {
        return ResponseEntity.ok(adminSystemStatusService.snapshot());
    }

    public ResponseEntity<?> systemProcesses() {
        return ResponseEntity.ok(systemDiagnosticsService.processes());
    }

    public ResponseEntity<?> systemDisk(String root, String path, int page) {
        try { return ResponseEntity.ok(systemDiagnosticsService.disk(root, path, page)); }
        catch (IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("message", error.getMessage())); }
    }

    public ResponseEntity<?> systemIncidents() {
        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "unacknowledgedCount", systemIncidentService.countUnacknowledged(),
                "list", systemIncidentService.findRecent()
        ));
    }

    public ResponseEntity<?> tokenSessions() {
        var sessions = refreshTokenService.findActiveSessions();
        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "count", sessions.size(),
                "list", sessions
        ));
    }

    public ResponseEntity<?> acknowledgeIncident(Long id) {
        systemIncidentService.acknowledge(id);
        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "message", "장애 기록을 확인 처리했습니다"
        ));
    }

    public ResponseEntity<?> acknowledgeAllIncidents() {
        int acknowledgedCount = systemIncidentService.acknowledgeAll();
        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "acknowledgedCount", acknowledgedCount,
                "message", acknowledgedCount > 0
                        ? acknowledgedCount + "건의 장애 기록을 모두 확인 처리했습니다"
                        : "확인할 장애 기록이 없습니다"
        ));
    }

    public ResponseEntity<?> forceTerminateAudioCall(String callId) {
        audioCallSignalingService.forceTerminate(callId);
        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "message", "통화를 강제로 종료했습니다"
        ));
    }

    public ResponseEntity<?> memberPermit(MemberRequestDto requestDto) {
        Long targetMemberId = requestDto.getId();

        if (targetMemberId == null && requestDto.getEmail() != null && !requestDto.getEmail().isBlank()) {
            targetMemberId = memberService.findMember(requestDto.getEmail()).getId();
        }

        memberService.activate(targetMemberId);

        return ResponseEntity.ok(Map.of(
                "result", memberService.updateRole(targetMemberId, Role.USER)
        ));
    }

    public ResponseEntity<?> memberDeny(MemberRequestDto requestDto) {
        Long targetMemberId = requestDto.getId();

        if (targetMemberId == null && requestDto.getEmail() != null && !requestDto.getEmail().isBlank()) {
            targetMemberId = memberService.findMember(requestDto.getEmail()).getId();
        }

        memberService.ban(targetMemberId);

        return ResponseEntity.ok(Map.of(
                "result", memberService.updateRole(targetMemberId, Role.DENIED)
        ));
    }

    public ResponseEntity<?> findAppeals(AppealStatus status, int page, int size) {
        Page<AppealResponseDto> appeals = appealService.findAll(status, page, size);

        return ResponseEntity.ok(Map.of(
                "result", appeals.getTotalElements(),
                "list", appeals.getContent(),
                "page", appeals.getNumber(),
                "totalPages", appeals.getTotalPages(),
                "totalElements", appeals.getTotalElements()
        ));
    }

    public ResponseEntity<?> acceptAppeal(Long appealId) {
        AppealResponseDto appeal = appealFacade.acceptAndUnblock(appealId);

        return ResponseEntity.ok(Map.of(
                "result", appeal.getId(),
                "appeal", appeal,
                "message", "회원 차단을 해제하고 상소를 승인했습니다"
        ));
    }

}
