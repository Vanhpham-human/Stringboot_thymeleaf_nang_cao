package com.example.library.service;

import com.example.library.dto.form.CategoryForm;
import com.example.library.entity.Category;
import com.example.library.exception.BusinessException;
import com.example.library.exception.DuplicateResourceException;
import com.example.library.mapper.CategoryMapper;
import com.example.library.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, new CategoryMapper());
    }

    @Test
    @DisplayName("Không cho tạo thể loại trùng tên (không phân biệt hoa thường)")
    void createRejectsDuplicateName() {
        when(categoryRepository.existsByNameIgnoreCase("Khoa học")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CategoryForm(null, "  Khoa học ", null)))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting("field").isEqualTo("name");
    }

    @Test
    @DisplayName("Không cho đổi tên trùng với thể loại khác")
    void updateRejectsDuplicateName() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category(true)));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Khoa học", 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(2L, new CategoryForm(2L, "Khoa học", null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Xóa mềm chuyển active = false, xóa lần hai báo lỗi")
    void softDelete() {
        Category category = category(true);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        service.softDelete(2L);

        assertThat(category.isActive()).isFalse();
        assertThatThrownBy(() -> service.softDelete(2L)).isInstanceOf(BusinessException.class);
    }

    private static Category category(boolean active) {
        Category category = new Category();
        category.setId(2L);
        category.setName("Văn học");
        category.setActive(active);
        return category;
    }
}
