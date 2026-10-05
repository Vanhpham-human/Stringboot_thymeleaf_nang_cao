package com.example.library.controller.api;

import com.example.library.dto.MemberDto;
import com.example.library.dto.PageResponse;
import com.example.library.dto.form.MemberForm;
import com.example.library.entity.enums.MemberStatus;
import com.example.library.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Members", description = "Quản lý thành viên")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @Operation(summary = "Danh sách thành viên, lọc theo trạng thái")
    @GetMapping
    public PageResponse<MemberDto> search(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) MemberStatus status,
                                          @ParameterObject @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(memberService.search(keyword, status, pageable));
    }

    @Operation(summary = "Xem chi tiết thành viên")
    @GetMapping("/{id}")
    public MemberDto get(@PathVariable Long id) {
        return memberService.getById(id);
    }

    @Operation(summary = "Tạo thành viên (email không được trùng)")
    @PostMapping
    public ResponseEntity<MemberDto> create(@Valid @RequestBody MemberForm form) {
        Long id = memberService.create(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.getById(id));
    }

    @Operation(summary = "Khóa thành viên")
    @PostMapping("/{id}/block")
    public MemberDto block(@PathVariable Long id) {
        memberService.block(id);
        return memberService.getById(id);
    }

    @Operation(summary = "Mở khóa thành viên")
    @PostMapping("/{id}/unblock")
    public MemberDto unblock(@PathVariable Long id) {
        memberService.unblock(id);
        return memberService.getById(id);
    }
}
