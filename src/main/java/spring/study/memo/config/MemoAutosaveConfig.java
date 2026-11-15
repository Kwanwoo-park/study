package spring.study.memo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class MemoAutosaveConfig {
    @Bean
    public ThreadPoolTaskScheduler memoAutosaveTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("memo-autosave-");
        scheduler.setRemoveOnCancelPolicy(true);

        return scheduler;
    }
}
