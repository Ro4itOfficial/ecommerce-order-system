package com.ecommerce.order.scheduler;

import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "app.scheduler.order-status-update.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class OrderStatusUpdateScheduler {

    private final OrderService orderService;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Scheduled(cron = "${app.scheduler.order-status-update.cron:0 */5 * * * *}")
    @SchedulerLock(name = "OrderStatusUpdateScheduler_processPendingOrders", 
                   lockAtLeastFor = "1m", 
                   lockAtMostFor = "4m")
    public void processPendingOrders() {
        log.info("Starting scheduled task: Process Pending Orders at {}", 
                LocalDateTime.now().format(DATE_TIME_FORMATTER));
        
        try {
            orderService.processPendingOrders();
            log.info("Completed scheduled task: Process Pending Orders");
        } catch (Exception e) {
            log.error("Error in scheduled task: Process Pending Orders", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM every day
    @SchedulerLock(name = "OrderStatusUpdateScheduler_cleanupOldOrders", 
                   lockAtLeastFor = "5m", 
                   lockAtMostFor = "55m")
    public void cleanupOldCancelledOrders() {
        log.info("Starting scheduled task: Cleanup Old Cancelled Orders at {}", 
                LocalDateTime.now().format(DATE_TIME_FORMATTER));
        
        try {
            int deletedCount = orderService.deleteOldCancelledOrders(30); // Delete orders cancelled more than 30 days ago
            log.info("Completed scheduled task: Deleted {} old cancelled orders", deletedCount);
        } catch (Exception e) {
            log.error("Error in scheduled task: Cleanup Old Cancelled Orders", e);
        }
    }

    @Scheduled(cron = "0 */30 * * * *") // Every 30 minutes
    public void logSystemStatus() {
        log.info("System Health Check - All schedulers are running normally at {}", 
                LocalDateTime.now().format(DATE_TIME_FORMATTER));
    }
}