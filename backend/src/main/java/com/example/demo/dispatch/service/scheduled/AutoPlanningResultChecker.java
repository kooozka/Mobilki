package com.example.demo.dispatch.service.scheduled;

import com.example.demo.dispatch.service.AutoPlanningAlgorithmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoPlanningResultChecker {

    private final AutoPlanningAlgorithmService autoPlanningService;

    @Scheduled(cron = "*/5 * * * * *")
    public void checkAutoPlanningResults() {
        autoPlanningService.checkAndProcessAutoPlanningResults();
    }
}
