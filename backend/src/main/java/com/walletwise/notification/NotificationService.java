package com.walletwise.notification;

import com.walletwise.common.ApiException;
import com.walletwise.common.PageResponse;
import com.walletwise.user.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final NotificationRepository notifications;
  private final CurrentUser currentUser;
  private final Clock clock;

  public NotificationService(
      NotificationRepository notifications, CurrentUser currentUser, Clock clock) {
    this.notifications = notifications;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PageResponse<NotificationResponse> list(Boolean unread, int page, int size) {
    UUID userId = currentUser.id();
    var pageable =
        PageRequest.of(
            Math.max(0, page),
            Math.min(Math.max(1, size), 100),
            Sort.by(Sort.Direction.DESC, "createdAt"));
    var result =
        Boolean.TRUE.equals(unread)
            ? notifications.findAllByOwnerIdAndReadAtIsNull(userId, pageable)
            : notifications.findAllByOwnerId(userId, pageable);
    return PageResponse.from(result.map(NotificationResponse::from));
  }

  @Transactional(readOnly = true)
  public UnreadCountResponse unreadCount() {
    return new UnreadCountResponse(notifications.countByOwnerIdAndReadAtIsNull(currentUser.id()));
  }

  @Transactional
  public NotificationResponse markRead(UUID id) {
    Notification notification =
        notifications
            .findByIdAndOwnerId(id, currentUser.id())
            .orElseThrow(() -> ApiException.notFound("Notification"));
    notification.markRead(Instant.now(clock));
    return NotificationResponse.from(notification);
  }

  @Transactional
  public UnreadCountResponse markAllRead() {
    notifications.markAllRead(currentUser.id(), Instant.now(clock));
    return new UnreadCountResponse(0);
  }

  public record NotificationResponse(
      UUID id,
      String type,
      String title,
      String message,
      boolean read,
      UUID relatedResourceId,
      Instant createdAt) {
    static NotificationResponse from(Notification notification) {
      return new NotificationResponse(
          notification.getId(),
          notification.getType().name(),
          notification.getTitle(),
          notification.getMessage(),
          notification.getReadAt() != null,
          notification.getRelatedResourceId(),
          notification.getCreatedAt());
    }
  }

  public record UnreadCountResponse(long unreadCount) {}
}
