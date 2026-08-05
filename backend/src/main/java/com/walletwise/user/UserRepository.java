package com.walletwise.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository
    extends JpaRepository<AppUser, UUID>, JpaSpecificationExecutor<AppUser> {
  Optional<AppUser> findByEmailNormalized(String emailNormalized);

  boolean existsByEmailNormalized(String emailNormalized);

  boolean existsByIdAndEnabledTrue(UUID id);
}
