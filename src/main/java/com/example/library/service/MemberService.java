package com.example.library.service;

import com.example.library.dto.MemberDto;
import com.example.library.dto.form.MemberForm;
import com.example.library.entity.Member;
import com.example.library.entity.enums.MemberStatus;
import com.example.library.exception.BusinessException;
import com.example.library.exception.DuplicateResourceException;
import com.example.library.exception.ResourceNotFoundException;
import com.example.library.mapper.MapperUtils;
import com.example.library.mapper.MemberMapper;
import com.example.library.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    public Page<MemberDto> search(String keyword, MemberStatus status, Pageable pageable) {
        return memberRepository.search(MapperUtils.trimToNull(keyword), status, pageable)
                .map(memberMapper::toDto);
    }

    public List<MemberDto> findActiveMembers() {
        return memberRepository.findByStatusOrderByFullNameAsc(MemberStatus.ACTIVE).stream()
                .map(memberMapper::toDto)
                .toList();
    }

    public MemberDto getById(Long id) {
        return memberMapper.toDto(findMember(id));
    }

    public MemberForm getForm(Long id) {
        return memberMapper.toForm(findMember(id));
    }

    @Transactional
    public Long create(MemberForm form) {
        String email = MemberMapper.normalizeEmail(form.getEmail());
        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw duplicateEmail(email);
        }
        Member member = new Member();
        memberMapper.updateEntity(form, member);
        return memberRepository.save(member).getId();
    }

    @Transactional
    public void update(Long id, MemberForm form) {
        Member member = findMember(id);
        String email = MemberMapper.normalizeEmail(form.getEmail());
        if (memberRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw duplicateEmail(email);
        }
        memberMapper.updateEntity(form, member);
    }

    @Transactional
    public MemberDto block(Long id) {
        Member member = findMember(id);
        if (member.isBlocked()) {
            throw new BusinessException("Thành viên " + member.getFullName() + " đã bị khóa trước đó");
        }
        member.setStatus(MemberStatus.BLOCKED);
        return memberMapper.toDto(member);
    }

    @Transactional
    public MemberDto unblock(Long id) {
        Member member = findMember(id);
        if (!member.isBlocked()) {
            throw new BusinessException("Thành viên " + member.getFullName() + " đang hoạt động");
        }
        member.setStatus(MemberStatus.ACTIVE);
        return memberMapper.toDto(member);
    }

    Member findMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("thành viên", id));
    }

    private DuplicateResourceException duplicateEmail(String email) {
        return new DuplicateResourceException("email", "Email " + email + " đã được sử dụng bởi thành viên khác");
    }
}
