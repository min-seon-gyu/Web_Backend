package com.WebProject.Member;

import com.WebProject.exception.BadRequestException;
import com.WebProject.jwt.Subject;
import io.jsonwebtoken.JwtException;
import io.netty.util.Constant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.common.util.impl.Log;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.authentication.PasswordEncoderParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.io.Console;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponse signUp(SignUpRequest signUpRequest) {
        if (memberRepository.existsByEmail(signUpRequest.getEmail()))
            throw new BadRequestException("이미 존재하는 이메일입니다.");
        if (memberRepository.findByNameAndRrn(signUpRequest.getName(), signUpRequest.getFrontRrn() + "-" + signUpRequest.getBackRrn()).isPresent())
            throw new BadRequestException("이미 회원가입 되어있습니다.");

        Member member = Member.builder()
                .email(signUpRequest.getEmail())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .name(signUpRequest.getName())
                .rrn(signUpRequest.getFrontRrn() + "-" + signUpRequest.getBackRrn())
                .number(signUpRequest.getNumber())
                .build();

        member = memberRepository.save(member);
        return MemberResponse.of(member);
    }

    @Transactional(readOnly = true)
    public MemberResponse login(LoginRequest loginRequest) {
        Member Member = memberRepository
                .findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("이메일 혹은 비밀번호를 확인하세요."));

        boolean matches = passwordEncoder.matches(
                loginRequest.getPassword(),
                Member.getPassword());
        if (!matches) throw new BadRequestException("이메일 혹은 비밀번호를 확인하세요.");

        return MemberResponse.of(Member);
    }

    @Transactional(readOnly = true)
    public MemberResponse findEmail(FindEmailRequest findEmailRequest){
        Member Member = memberRepository
                .findByNameAndRrn(findEmailRequest.getName(), findEmailRequest.getFrontRrn()+"-"+findEmailRequest.getBackRrn())
                .orElseThrow(() -> new BadRequestException("이름 혹은 주민등록번호를 확인하세요."));

        return MemberResponse.of(Member);
    }

    @Transactional(readOnly = true)
    public boolean findPassword(FindPasswordRequest findPasswordRequest){
        boolean isValid = memberRepository
                .existsByEmailAndNameAndRrn(findPasswordRequest.getEmail(), findPasswordRequest.getName(), findPasswordRequest.getFrontRrn()+"-"+findPasswordRequest.getBackRrn());

        return isValid;
    }

    @Transactional(readOnly = true)
    public List<Member> findAll(){
        return memberRepository.findAll();
    }

    @Transactional(readOnly = false)
    public void delete(Member member){
        memberRepository.delete(member);
    }

    @Transactional(readOnly = false)
    public MemberResponse update(String email, UpdateRequest updateRequest){

        Optional<Member> member = memberRepository.findByEmail(email);

        member.ifPresent(selectMember -> {
            selectMember.setEmail(updateRequest.getEmail());

            if(updateRequest.getName() != null){
                selectMember.setName(updateRequest.getName());
            }

            if(updateRequest.getNumber() != null){
                selectMember.setNumber(updateRequest.getNumber());
            }

            if(updateRequest.getFrontRrn() != null && updateRequest.getBackRrn() != null){
                selectMember.setRrn(updateRequest.getFrontRrn() + "-" + updateRequest.getBackRrn());
            }

            if(updateRequest.getPassword() != null){
                String encodedPassword = passwordEncoder.encode(updateRequest.getPassword());
                selectMember.setPassword(encodedPassword);
            }

            selectMember = memberRepository.save(selectMember);
        });
        return MemberResponse.of(member.get());
    }

    @Transactional(readOnly = true)
    public Long totalCount(){
        return memberRepository.count();
    }

    @Transactional(readOnly = false)
    public MemberResponse passwordUpdate(PasswordUpdateRequest passwordUpdateRequest) {
        Optional<Member> member = memberRepository.findByEmail(passwordUpdateRequest.getEmail());

        member.ifPresent(selectMember -> {
            selectMember.setEmail(passwordUpdateRequest.getEmail());

            if(passwordUpdateRequest.getPassword() != null){
                String encodedPassword = passwordEncoder.encode(passwordUpdateRequest.getPassword());
                selectMember.setPassword(encodedPassword);
            }

            selectMember = memberRepository.save(selectMember);
        });
        return MemberResponse.of(member.get());
    }
}
