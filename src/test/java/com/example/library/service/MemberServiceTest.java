package com.example.library.service;

import com.example.library.dto.form.MemberForm;
import com.example.library.entity.Member;
import com.example.library.entity.enums.MemberStatus;
import com.example.library.exception.BusinessException;
import com.example.library.exception.DuplicateResourceException;
import com.example.library.mapper.MemberMapper;
import com.example.library.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    private MemberService service;

    @BeforeEach
    void setUp() {
        service = new MemberService(memberRepository, new MemberMapper());
    }

    @Test
    @DisplayName("Email được chuẩn hóa chữ thường và không được trùng")
    void createRejectsDuplicateEmail() {
        when(memberRepository.existsByEmailIgnoreCase("an@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(form("  AN@Example.com ")))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting("field").isEqualTo("email");
    }

    @Test
    @DisplayName("Tạo thành viên lưu email đã chuẩn hóa")
    void createNormalizesEmail() {
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(3L);
            return m;
        });

        Long id = service.create(form("Binh@Example.COM"));

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertThat(id).isEqualTo(3L);
        assertThat(captor.getValue().getEmail()).isEqualTo("binh@example.com");
    }

    @Test
    @DisplayName("Khóa và mở khóa thành viên")
    void blockAndUnblock() {
        Member member = new Member();
        member.setId(1L);
        member.setFullName("Nguyễn Văn An");
        member.setStatus(MemberStatus.ACTIVE);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        service.block(1L);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.BLOCKED);
        assertThatThrownBy(() -> service.block(1L)).isInstanceOf(BusinessException.class);

        service.unblock(1L);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    private static MemberForm form(String email) {
        return MemberForm.builder()
                .fullName("Nguyễn Văn An")
                .email(email)
                .phone("0912345678")
                .status(MemberStatus.ACTIVE)
                .build();
    }
}
