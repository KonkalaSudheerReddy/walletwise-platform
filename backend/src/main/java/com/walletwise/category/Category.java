package com.walletwise.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category {
  public enum Type {
    INCOME,
    EXPENSE
  }

  @Id private UUID id;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(name = "normalized_name", nullable = false, length = 80)
  private String normalizedName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Type type;

  @Column(nullable = false)
  private boolean active;

  protected Category() {}

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getNormalizedName() {
    return normalizedName;
  }

  public Type getType() {
    return type;
  }

  public boolean isActive() {
    return active;
  }
}
