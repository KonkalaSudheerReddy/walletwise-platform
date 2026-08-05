package com.walletwise.ledger;

import java.time.Instant;
import java.util.UUID;

public record ExpenseRecordedEvent(UUID ownerId, UUID categoryId, Instant occurredAt) {}
