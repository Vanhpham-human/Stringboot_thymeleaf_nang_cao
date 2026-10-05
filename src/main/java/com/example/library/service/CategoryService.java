package com.example.library.service;

import com.example.library.dto.CategoryDto;
import com.example.library.dto.form.CategoryForm;
import com.example.library.entity.Category;
import com.example.library.exception.BusinessException;
import com.example.library.exception.DuplicateResourceException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.mapper.CategoryMapper;
import com.example.library.mapper.MapperUtils;
import com.example.library.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public Page<CategoryDto> search(String keyword, Boolean active, Pageable pageable) {
        Page<Category> page = categoryRepository.search(MapperUtils.trimToNull(keyword), active, pageable);
        Map<Long, Long> bookCounts = countBooks(page.getContent());
        return page.map(c -> categoryMapper.toDto(c, bookCounts.getOrDefault(c.getId(), 0L)));
    }

    public List<CategoryDto> findAllActive() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    public CategoryDto getById(Long id) {
        return categoryMapper.toDto(findCategory(id));
    }

    public CategoryForm getForm(Long id) {
        return categoryMapper.toForm(findCategory(id));
    }

    @Transactional
    public Long create(CategoryForm form) {
        String name = form.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw duplicateName(name);
        }
        Category category = new Category();
        categoryMapper.updateEntity(form, category);
        category.setActive(true);
        return categoryRepository.save(category).getId();
    }

    @Transactional
    public void update(Long id, CategoryForm form) {
        Category category = findCategory(id);
        String name = form.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicateName(name);
        }
        categoryMapper.updateEntity(form, category);
    }

    /** Xóa mềm: thể loại bị ẩn khỏi form tạo sách nhưng dữ liệu sách cũ vẫn giữ nguyên. */
    @Transactional
    public void softDelete(Long id) {
        Category category = findCategory(id);
        if (!category.isActive()) {
            throw new BusinessException("Thể loại \"" + category.getName() + "\" đã bị xóa trước đó");
        }
        category.setActive(false);
    }

    @Transactional
    public void restore(Long id) {
        findCategory(id).setActive(true);
    }

    Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("thể loại", id));
    }

    private Map<Long, Long> countBooks(List<Category> categories) {
        Map<Long, Long> result = new HashMap<>();
        if (categories.isEmpty()) {
            return result;
        }
        List<Long> ids = categories.stream().map(Category::getId).toList();
        for (Object[] row : categoryRepository.countBooksByCategoryIds(ids)) {
            result.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    private DuplicateResourceException duplicateName(String name) {
        return new DuplicateResourceException("name", "Tên thể loại \"" + name + "\" đã tồn tại");
    }
}
