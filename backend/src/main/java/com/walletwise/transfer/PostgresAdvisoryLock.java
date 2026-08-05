package com.walletwise.transfer;

import jakarta.persistence.EntityManager;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

@Component
public class PostgresAdvisoryLock {
  private final EntityManager entityManager;

  public PostgresAdvisoryLock(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public void acquire(String scope) {
    entityManager
        .createNativeQuery("select pg_advisory_xact_lock(?1)")
        .setParameter(1, lockKey(scope))
        .getSingleResult();
  }

  private static long lockKey(String scope) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(scope.getBytes(StandardCharsets.UTF_8));
      return ByteBuffer.wrap(digest).getLong();
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
