package com.example.cartrecovery.controller;


import com.example.cartrecovery.scheduler.AbandonedCartScheduler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class SchedulerTestController {

    private final AbandonedCartScheduler abandonedCartScheduler;


    public SchedulerTestController(AbandonedCartScheduler abandonedCartScheduler) {
        this.abandonedCartScheduler = abandonedCartScheduler;
    }

    @PostMapping("/run-reminder")
    public String runReminderSchduler(){
        abandonedCartScheduler.checkForAbandonedCarts();

        return "First reminder scheduler executed successfully.";
    }

    @PostMapping("/run-second-reminder")
    public String runSecondReminderScheduler() {
        abandonedCartScheduler.sendDueSecondReminders();
        return "Second reminder scheduler executed successfully.";
    }
}
