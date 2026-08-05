package com.walletwise.category;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
  List<Category> findAllByTypeAndActiveTrueOrderByName(Category.Type type);

  List<Category> findAllByActiveTrueOrderByTypeAscNameAsc();

  java.util.Optional<Category> findByNormalizedNameAndType(
      String normalizedName, Category.Type type);
}
