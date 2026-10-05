package com.example.library.service;

import com.example.library.config.BorrowingProperties;
import com.example.library.dto.form.FinePolicyForm;
import com.example.library.entity.FinePolicy;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.FineMapper;
import com.example.library.repository.FinePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinePolicyServiceTest {

    @Mock
    private FinePolicyRepository finePolicyRepository;

    private FinePolicyService service;

    @BeforeEach
    void setUp() {
        service = new FinePolicyService(finePolicyRepository,
                new BorrowingProperties(14, 5, BigDecimal.valueOf(5000)), new FineMapper());
    }

    @Test
    @DisplayName("Chưa có policy active -> dùng mức phí mặc định trong cấu hình")
    void fallsBackToConfiguredDefault() {
        when(finePolicyRepository.findFirstByActiveTrue()).thenReturn(Optional.empty());

        FinePolicy policy = service.getActivePolicy();

        assertThat(policy.getId()).isNull();
        assertThat(policy.getDailyFineAmount()).isEqualByComparingTo("5000");
        assertThat(policy.getMaxFineAmount()).isNull();
        assertThat(policy.getGraceDays()).isZero();
    }

    @Test
    @DisplayName("Tạo policy và áp dụng ngay -> tắt các policy khác trước khi lưu")
    void createActiveDeactivatesOthersFirst() {
        when(finePolicyRepository.save(any(FinePolicy.class))).thenAnswer(inv -> {
            FinePolicy p = inv.getArgument(0);
            p.setId(3L);
            return p;
        });

        Long id = service.create(form(7000, 150000, true));

        assertThat(id).isEqualTo(3L);
        InOrder order = inOrder(finePolicyRepository);
        order.verify(finePolicyRepository).deactivateAll();
        ArgumentCaptor<FinePolicy> captor = ArgumentCaptor.forClass(FinePolicy.class);
        order.verify(finePolicyRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue().getDailyFineAmount()).isEqualByComparingTo("7000");
    }

    @Test
    @DisplayName("Lưu policy không áp dụng ngay thì không đụng tới policy đang active")
    void createInactiveKeepsCurrentPolicy() {
        when(finePolicyRepository.save(any(FinePolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(form(7000, null, false));

        verify(finePolicyRepository, never()).deactivateAll();
    }

    @Test
    @DisplayName("Mức trần nhỏ hơn phí một ngày bị từ chối")
    void rejectsMaxLowerThanDaily() {
        assertThatThrownBy(() -> service.create(form(5000, 3000, true)))
                .isInstanceOf(BusinessException.class)
                .extracting("field").isEqualTo("maxFineAmount");
        verify(finePolicyRepository, never()).save(any());
    }

    private static FinePolicyForm form(long daily, Integer max, boolean activate) {
        return FinePolicyForm.builder()
                .name("Chính sách 2027")
                .dailyFineAmount(BigDecimal.valueOf(daily))
                .maxFineAmount(max == null ? null : BigDecimal.valueOf(max))
                .graceDays(0)
                .activate(activate)
                .build();
    }
}
