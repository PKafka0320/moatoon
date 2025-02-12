package com._2.a401.moa.member.service;

import com._2.a401.moa.auth.exception.AuthException;
import com._2.a401.moa.auth.service.MailService;
import com._2.a401.moa.common.exception.MoaException;
import com._2.a401.moa.member.domain.Member;
import com._2.a401.moa.member.domain.MemberRole;
import com._2.a401.moa.member.dto.request.FindPwRequest;
import com._2.a401.moa.member.dto.request.FindIdRequest;
import com._2.a401.moa.member.dto.request.MemberCreate;
import com._2.a401.moa.member.dto.response.FindIdInfo;
import com._2.a401.moa.member.dto.response.SearchChildInfo;
import com._2.a401.moa.member.dto.response.MemberInfoResponse;
import com._2.a401.moa.member.repository.MemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com._2.a401.moa.common.exception.ExceptionCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Transactional
    public void createMember(MemberCreate memberCreate) {
        if (memberCreate.isChild()) {
            createChildren(memberCreate);
        } else {
            createManager(memberCreate);
        }
    }

    private void createChildren(MemberCreate memberCreate) {
        memberRepository.save(memberCreate.toChildMember(passwordEncoder));
    }

    private void createManager(MemberCreate memberCreate) {
        Member manager = memberRepository.save(memberCreate.toManageMember(passwordEncoder));
        if (memberCreate.getChildren() != null) {
            List<Member> members = memberCreate.getChildren().stream()
                    .map(this::getMember)
                    .toList();

            members.forEach(child -> child.setManager(manager));
        }
    }

    private Member getMember(Long childId) {
        return memberRepository.findById(childId)
                .orElseThrow(() -> new MoaException(INVALID_MEMBER));
    }

    public MemberInfoResponse getUserInfo(long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MoaException(INVALID_MEMBER));

        if (member.getRole() == MemberRole.MANAGER) {
            List<Member> children = memberRepository.findByManager(member);
            return MemberInfoResponse.ofManager(member, children);
        }else if(member.getManager()==null){
            throw new MoaException(UNCONNECTED_CHILD);
        }

        return MemberInfoResponse.ofChild(member);
    }

    public SearchChildInfo getChildInfoById(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new MoaException(INVALID_MEMBER));

        if (!member.getRole().equals(MemberRole.CHILD)) {
            throw new MoaException(INVALID_MEMBER);
        }
        if (member.getManager()!=null) {
            throw new MoaException(DUPLICATED_CHILD);
        }
        return SearchChildInfo.from(member);
    }

    @Transactional
    public void sendUserPwMail(FindPwRequest req) {
        Member member = memberRepository.findByLoginId(req.loginId())
                .orElseThrow(()->new MoaException(INVALID_MEMBER));;
        if(member.getRole().equals(MemberRole.CHILD)){
            if(!member.getManager().getEmail().equals(req.email())){
                throw new MoaException(INVALID_MEMBER);
            }
        }else{
            if(!member.getEmail().equals(req.email())){
                throw new MoaException(INVALID_MEMBER);
            }
        }

        String pw = mailService.sendPwMail(req.email());

        member.setPassword(passwordEncoder.encode(pw));
    }

    public FindIdInfo getLoginIdByNameAndEmail(FindIdRequest req) {
        Member member = memberRepository.findByEmailAndName(req.email(), req.name())
                .orElseThrow(()->new MoaException(INVALID_MEMBER));

        return FindIdInfo.from(member);
    }
}
