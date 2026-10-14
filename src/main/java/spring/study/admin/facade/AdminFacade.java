package spring.study.admin.facade;

import com.sun.management.OperatingSystemMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.admin.dto.AdminNewBoardResponseDto;
import spring.study.admin.service.SystemIncidentService;
import spring.study.admin.service.SystemDiagnosticsService;
import spring.study.appeal.dto.AppealResponseDto;
import spring.study.appeal.entity.Appeal;
import spring.study.appeal.entity.AppealStatus;
import spring.study.appeal.service.AppealService;
import spring.study.aws.service.ImageCleanupService;
import spring.study.board.service.BoardService;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.AudioCallSignalingService;
import spring.study.chat.service.ChatPresenceService;
import spring.study.jwt.dto.AdminTokenSessionResponse;
import spring.study.kafka.service.KafkaQueueQueryService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.sanction.service.MemberSanctionService;
import spring.study.member.service.MemberService;
import spring.study.common.service.OnlineUserService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.kafka.dto.KafkaQueueCounts;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFacade {
    private final MemberService memberService;
    private final MemberSanctionService memberSanctionService;
    private final BoardService boardService;
    private final AppealService appealService;
    private final spring.study.appeal.facade.AppealFacade appealFacade;
    private final ChatMessageService chatMessageService;
    private final SystemIncidentService systemIncidentService;
    private final AudioCallSignalingService audioCallSignalingService;
    private final OnlineUserService onlineUserService;
    private final RefreshTokenService refreshTokenService;
    private final ChatPresenceService chatPresenceService;
    private final KafkaQueueQueryService kafkaQueueQueryService;
    private final ImageCleanupService imageCleanupService;
    private final SystemDiagnosticsService systemDiagnosticsService;
    private final NotificationService notificationService;

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
        Map<String, Object> status = new HashMap<>();

        status.put("result", 1L);
        status.put("cpu", CpuUsedPercent());
        status.put("memory", MemoryUsedPercent());
        status.put("disk", DiskUsedPercent());
        status.put("jvm", JVMHeap());
        status.put("traffic", Traffic());
        status.put("queues", KafkaQueue());

        return ResponseEntity.ok(status);
    }

    public ResponseEntity<?> systemProcesses() {
        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "process", systemDiagnosticsService.processes()
        ));
    }

    public ResponseEntity<?> systemDisk(String root, String path, int page) {
        try {
            return ResponseEntity.ok(Map.of(
                    "result", 1L,
                    "disk", systemDiagnosticsService.disk(root, path, page)
            ));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }

    public ResponseEntity<?> systemIncidents() {
        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "unacknowledgedCount", systemIncidentService.countUnacknowledged(),
                "list", systemIncidentService.findRecent()
        ));
    }

    public ResponseEntity<?> tokenSessions() {
        List<AdminTokenSessionResponse> sessions = refreshTokenService.findActiveSessions();
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
        Appeal appeal = appealService.accept(appealId);

        memberSanctionService.unblock(appeal.getMember(), appeal.getRelatedSanction());
        notificationService.createNotification(appeal.getMember(), "상소가 받아들여져 계정 차단이 해제되었습니다", Group.ADMIN);

        return ResponseEntity.ok(Map.of(
                "result", appeal.getId(),
                "message", "회원 차단을 해제하고 상소를 승인했습니다"
        ));
    }

    private Map<String, Object> CpuUsedPercent() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        return Map.of(
                "systemPercent", percent(osBean.getCpuLoad()),
                "processPercent", percent(osBean.getProcessCpuLoad()),
                "cores", osBean.getAvailableProcessors()
        );
    }

    private Map<String, Object> MemoryUsedPercent() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        long usedMemory = totalMemory - freeMemory;

        return Map.of(
                "usedBytes", usedMemory,
                "totalBytes", totalMemory,
                "usedPercent", percent(usedMemory, totalMemory)
        );
    }

    private Map<String, Object> DiskUsedPercent() {
        File root = new File("/");

        long totalDisk = root.getTotalSpace();
        long freeDisk = root.getFreeSpace();
        long usedDisk = totalDisk - freeDisk;

        return Map.of(
                "usedBytes", usedDisk,
                "totalBytes", totalDisk,
                "usedPercent", percent(usedDisk, totalDisk)
        );
    }

    private Map<String, Object> JVMHeap() {
        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();

        long maxHeap = runtime.maxMemory();
        long usedHeap = runtime.totalMemory() - runtime.freeMemory();

        return Map.of(
                "heapUsedBytes", usedHeap,
                "heapMaxBytes", maxHeap,
                "heapUsedPercent", percent(usedHeap, maxHeap),
                "uptimeMillis", runtimeMxBean.getUptime(),
                "uptime", formatDuration(Duration.ofMillis(runtimeMxBean.getUptime())),
                "startedAt", LocalDateTime.now().minus(Duration.ofMillis(runtimeMxBean.getUptime())).toString()
        );
    }

    private Map<String, Object> Traffic() {
        return Map.of(
                "activeSessions", onlineUserService.countOnlineMembers(),
                "activeWebSockets", chatPresenceService.countActiveConnections()
        );
    }

    private Map<String, Object> KafkaQueue() {
        KafkaQueueCounts kafka = kafkaQueueQueryService.counts();

        return Map.of(
                "kafkaPending", kafka.pending(),
                "kafkaDeadLetters", kafka.failed(),
                "imageCleanupPending", imageCleanupService.countPending()
        );
    }

    private double percent(double ratio) {
        if (Double.isNaN(ratio) || ratio < 0) {
            return 0D;
        }

        return Math.round(ratio * 1000D) / 10D;
    }

    private double percent(long used, long total) {
        if (total <= 0) {
            return 0D;
        }

        return Math.round((used * 1000D) / total) / 10D;
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days > 0) {
            return days + "일 " + hours + "시간 " + minutes + "분";
        }

        if (hours > 0) {
            return hours + "시간 " + minutes + "분";
        }

        return minutes + "분";
    }
}
