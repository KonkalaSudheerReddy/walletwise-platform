package com.walletwise.notification;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  Optional<Notification> findByIdAndOwnerId(UUID id, UUID ownerId);

  Page<Notification> findAllByOwnerId(UUID ownerId, Pageable pageable);

  Page<Notification> findAllByOwnerIdAndReadAtIsNull(UUID ownerId, Pageable pageable);

  long countByOwnerIdAndReadAtIsNull(UUID ownerId);

  @Modifying
  @Query(
      "update Notification n set n.readAt = :now where n.owner.id = :ownerId and n.readAt is null")
  int markAllRead(@Param("ownerId") UUID ownerId, @Param("now") Instant now);

  @Modifying
  @Query(
      value =
          """
            insert into notifications
                (id, owner_id, type, title, message, related_resource_id, budget_id, read_at, created_at)
            values (:id, :ownerId, :type, :title, :message, :budgetId, :budgetId, null, :createdAt)
            on conflict (budget_id, type) where budget_id is not null do nothing
            """,
      nativeQuery = true)
  int insertBudgetNotification(
      @Param("id") UUID id,
      @Param("ownerId") UUID ownerId,
      @Param("type") String type,
      @Param("title") String title,
      @Param("message") String message,
      @Param("budgetId") UUID budgetId,
      @Param("createdAt") Instant createdAt);
}
