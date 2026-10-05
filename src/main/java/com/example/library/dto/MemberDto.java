package com.example.library.dto;

import com.example.library.entity.enums.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private MemberStatus status;
    private LocalDateTime createdAt;
}
