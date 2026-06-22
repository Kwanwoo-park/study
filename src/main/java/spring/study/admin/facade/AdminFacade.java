package spring.study.admin.facade;

import com.sun.management.OperatingSystemMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardService;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.service.ChatMessageService;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFacade {
    private final MemberService memberService;
    private final BoardService boardService;
    private final ChatMessageService chatMessageService;
    private final RedisTemplate<String, String> redisTemplate;

    public ResponseEntity<?> memberOnline() {
        Set<String> onlineUserKeys = redisTemplate.keys("online:user:*");

        List<Long> userIds = (onlineUserKeys == null ? Set.<String>of() : onlineUserKeys)
                .stream()
                .map(k -> k.substring(k.lastIndexOf(":") + 1))
                .map(Long::parseLong)
                .toList();
        long count = userIds.size();

        if (count == 0L) {
            redisTemplate.delete("online:total");
        } else {
            redisTemplate.opsForValue().set("online:total", Long.toString(count));
        }

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

        List<Board> list = boardService.findNewBoard(start, end);
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
        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        File root = new File("/");

        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        long usedMemory = Math.max(0L, totalMemory - freeMemory);
        long maxHeap = runtime.maxMemory();
        long usedHeap = runtime.totalMemory() - runtime.freeMemory();
        long totalDisk = root.getTotalSpace();
        long freeDisk = root.getFreeSpace();
        long usedDisk = Math.max(0L, totalDisk - freeDisk);

        Map<String, Object> status = new HashMap<>();
        status.put("result", 1L);
        status.put("cpu", Map.of(
                "systemPercent", percent(osBean.getCpuLoad()),
                "processPercent", percent(osBean.getProcessCpuLoad()),
                "cores", osBean.getAvailableProcessors()
        ));
        status.put("memory", Map.of(
                "usedBytes", usedMemory,
                "totalBytes", totalMemory,
                "usedPercent", percent(usedMemory, totalMemory)
        ));
        status.put("disk", Map.of(
                "usedBytes", usedDisk,
                "totalBytes", totalDisk,
                "usedPercent", percent(usedDisk, totalDisk)
        ));
        status.put("jvm", Map.of(
                "heapUsedBytes", usedHeap,
                "heapMaxBytes", maxHeap,
                "heapUsedPercent", percent(usedHeap, maxHeap),
                "uptimeMillis", runtimeMxBean.getUptime(),
                "uptime", formatDuration(Duration.ofMillis(runtimeMxBean.getUptime())),
                "startedAt", LocalDateTime.now().minus(Duration.ofMillis(runtimeMxBean.getUptime())).toString()
        ));
        status.put("traffic", Map.of(
                "activeSessions", countKeys("online:user:*"),
                "activeWebSockets", countKeys("chat:room:*:active:*")
        ));

        return ResponseEntity.ok(status);
    }

    private long countKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);

        return keys.size();
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
