package com.example.library.mapper;

import com.example.library.dto.MemberDto;
import com.example.library.dto.form.MemberForm;
import com.example.library.entity.Member;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MemberMapper {

    public MemberDto toDto(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .status(member.getStatus())
                .createdAt(member.getCreatedAt())
                .build();
    }

    public MemberForm toForm(Member member) {
        return MemberForm.builder()
                .id(member.getId())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .status(member.getStatus())
                .build();
    }

    public void updateEntity(MemberForm form, Member member) {
        member.setFullName(form.getFullName().trim());
        member.setEmail(normalizeEmail(form.getEmail()));
        member.setPhone(MapperUtils.trimToNull(form.getPhone()));
        member.setStatus(form.getStatus());
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
