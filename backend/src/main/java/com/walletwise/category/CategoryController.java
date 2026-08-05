package com.walletwise.category;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
  private final CategoryRepository categories;

  public CategoryController(CategoryRepository categories) {
    this.categories = categories;
  }

  @GetMapping
  List<CategoryResponse> list(@RequestParam(required = false) Category.Type type) {
    List<Category> result =
        type == null
            ? categories.findAllByActiveTrueOrderByTypeAscNameAsc()
            : categories.findAllByTypeAndActiveTrueOrderByName(type);
    return result.stream().map(CategoryResponse::from).toList();
  }

  public record CategoryResponse(UUID id, String name, String type) {
    static CategoryResponse from(Category category) {
      return new CategoryResponse(category.getId(), category.getName(), category.getType().name());
    }
  }
}
