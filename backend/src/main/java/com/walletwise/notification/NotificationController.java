package com.walletwise.notification;

import com.walletwise.common.PageResponse;
import com.walletwise.notification.NotificationService.NotificationResponse;
import com.walletwise.notification.NotificationService.UnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Idempotent budget-alert notifications")
public class NotificationController {
  private final NotificationService notifications;

  public NotificationController(NotificationService notifications) {
    this.notifications = notifications;
  }

  @GetMapping
  @Operation(summary = "List notifications with unread and pagination filters")
  PageResponse<NotificationResponse> list(
      @RequestParam(required = false) Boolean unread,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return notifications.list(unread, page, size);
  }

  @GetMapping("/unread-count")
  @Operation(summary = "Count unread notifications")
  UnreadCountResponse count() {
    return notifications.unreadCount();
  }

  @PatchMapping("/{id}/read")
  @Operation(summary = "Mark one notification read")
  NotificationResponse read(@PathVariable UUID id) {
    return notifications.markRead(id);
  }

  @PatchMapping("/read-all")
  @Operation(summary = "Mark every owned notification read")
  UnreadCountResponse readAll() {
    return notifications.markAllRead();
  }
}
