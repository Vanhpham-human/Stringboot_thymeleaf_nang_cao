package com.example.library.mapper;

import com.example.library.dto.CategoryDto;
import com.example.library.dto.form.CategoryForm;
import com.example.library.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryDto toDto(Category category, long bookCount) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.isActive())
                .bookCount(bookCount)
                .createdAt(category.getCreatedAt())
                .build();
    }

    public CategoryDto toDto(Category category) {
        return toDto(category, 0);
    }

    public CategoryForm toForm(Category category) {
        return CategoryForm.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    public void updateEntity(CategoryForm form, Category category) {
        category.setName(form.getName().trim());
        category.setDescription(MapperUtils.trimToNull(form.getDescription()));
    }
}
