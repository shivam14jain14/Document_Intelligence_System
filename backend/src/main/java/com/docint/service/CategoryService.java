package com.docint.service;

import com.docint.dto.request.CategoryRequest;
import com.docint.dto.response.CategoryResponse;
import com.docint.entity.Category;
import com.docint.exception.ConflictException;
import com.docint.exception.ResourceNotFoundException;
import com.docint.config.CacheConfig;
import com.docint.repository.CategoryRepository;
import com.docint.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;

    @Cacheable(CacheConfig.CATEGORIES)
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        Map<String, Long> counts = documentRepository.countByCategory().stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(c -> CategoryResponse.from(c, counts.getOrDefault(c.getName(), 0L)))
                .toList();
    }

    @CacheEvict(value = CacheConfig.CATEGORIES, allEntries = true)
    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        if (categoryRepository.existsByName(req.name())) {
            throw new ConflictException("Category already exists: " + req.name());
        }
        Category saved = categoryRepository.save(Category.builder()
                .name(req.name())
                .description(req.description())
                .build());
        return CategoryResponse.from(saved, 0);
    }

    @CacheEvict(value = CacheConfig.CATEGORIES, allEntries = true)
    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        category.setName(req.name());
        category.setDescription(req.description());
        return CategoryResponse.from(categoryRepository.save(category), 0);
    }

    @CacheEvict(value = CacheConfig.CATEGORIES, allEntries = true)
    @Transactional
    public void delete(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found: " + id);
        }
        categoryRepository.deleteById(id);
    }
}
