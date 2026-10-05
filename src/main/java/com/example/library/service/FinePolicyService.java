package com.example.library.service;

import com.example.library.config.BorrowingProperties;
import com.example.library.dto.FinePolicyDto;
import com.example.library.dto.form.FinePolicyForm;
import com.example.library.entity.FinePolicy;
import com.example.library.exception.BusinessException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.mapper.FineMapper;
import com.example.library.mapper.MapperUtils;
import com.example.library.repository.FinePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinePolicyService {

    private final FinePolicyRepository finePolicyRepository;
    private final BorrowingProperties borrowingProperties;
    private final FineMapper fineMapper;

    public List<FinePolicyDto> findAll() {
        return finePolicyRepository.findAllByOrderByActiveDescCreatedAtDesc().stream()
                .map(fineMapper::toDto)
                .toList();
    }

    /**
     * Policy đang áp dụng. Nếu chưa kích hoạt policy nào, dùng mức phí dự phòng trong application.yml
     * (không giới hạn trần, không miễn ngày) để nghiệp vụ trả sách không bị gián đoạn.
     */
    public FinePolicy getActivePolicy() {
        return finePolicyRepository.findFirstByActiveTrue().orElseGet(() -> {
            log.warn("Chưa có chính sách phí phạt active, dùng mức phí mặc định {}", borrowingProperties.defaultFinePerDay());
            FinePolicy fallback = new FinePolicy("Mặc định hệ thống", borrowingProperties.defaultFinePerDay(), null, 0);
            fallback.setActive(true);
            return fallback;
        });
    }

    public FinePolicyDto getActivePolicyDto() {
        return fineMapper.toDto(getActivePolicy());
    }

    public FinePolicyForm newForm() {
        FinePolicyDto active = getActivePolicyDto();
        return FinePolicyForm.builder()
                .dailyFineAmount(active.getDailyFineAmount())
                .maxFineAmount(active.getMaxFineAmount())
                .graceDays(active.getGraceDays())
                .activate(true)
                .build();
    }

    @Transactional
    public Long create(FinePolicyForm form) {
        if (form.getMaxFineAmount() != null && form.getMaxFineAmount().compareTo(form.getDailyFineAmount()) < 0) {
            throw new BusinessException("maxFineAmount", "Mức trần phải lớn hơn hoặc bằng phí một ngày");
        }
        if (form.isActivate()) {
            finePolicyRepository.deactivateAll();
        }
        FinePolicy policy = new FinePolicy(form.getName().trim(), form.getDailyFineAmount(), form.getMaxFineAmount(),
                form.getGraceDays() == null ? 0 : form.getGraceDays());
        policy.setDescription(MapperUtils.trimToNull(form.getDescription()));
        policy.setActive(form.isActivate());
        FinePolicy saved = finePolicyRepository.save(policy);
        log.info("Tạo chính sách phí #{} ({}), active={}", saved.getId(), saved.getName(), saved.isActive());
        return saved.getId();
    }

    @Transactional
    public FinePolicyDto activate(Long id) {
        if (!finePolicyRepository.existsById(id)) {
            throw ResourceNotFoundException.of("chính sách phí", id);
        }
        finePolicyRepository.deactivateAll();
        FinePolicy policy = finePolicyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("chính sách phí", id));
        policy.setActive(true);
        log.info("Kích hoạt chính sách phí #{} ({})", policy.getId(), policy.getName());
        return fineMapper.toDto(policy);
    }
}
