package spring.study.todo.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import spring.study.diary.service.DiaryService;
import spring.study.member.entity.Member;
import spring.study.todo.service.TodoService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
public class WeekReviewFacade {
    private final DiaryService diaryService;
    private final TodoService todoService;

    public String week(Member member, Model model, int weeksAgo) {
        int offset = Math.max(0, Math.min(weeksAgo, 52));
        LocalDate start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset);
        LocalDate end = start.plusWeeks(1);
        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("weekStart", start);
        model.addAttribute("weekEnd", end.minusDays(1));
        model.addAttribute("weeksAgo", offset);
        model.addAttribute("completed", todoService.findCompletedForWeek(member, start, end));
        model.addAttribute("diaries", diaryService.findForWeek(member, start, end));
        return "todo/week";
    }
}
