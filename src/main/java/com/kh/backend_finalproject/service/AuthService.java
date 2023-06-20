package com.kh.backend_finalproject.service;

import com.kh.backend_finalproject.constant.Authority;
import com.kh.backend_finalproject.constant.IsActive;
import com.kh.backend_finalproject.dto.token.TokenDto;
import com.kh.backend_finalproject.dto.UserRequestDto;
import com.kh.backend_finalproject.dto.UserResponseDto;
import com.kh.backend_finalproject.entitiy.UserTb;
import com.kh.backend_finalproject.jwt.TokenProvider;
import com.kh.backend_finalproject.repository.UserRepository;
import com.kh.backend_finalproject.utils.TokenExpiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final AuthenticationManagerBuilder managerBuilder;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    @Autowired
    EmailService emailService;

    @Autowired
    JavaMailSender javaMailSender;

    // 🔐회원가입
    public UserResponseDto join(UserRequestDto requestDto) throws Exception {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new RuntimeException("이미 가입되어 있는 사용자 압니다. 🐿️");
        }

        String authKey = emailService.sendSimpleMessage(requestDto.getEmail());
        requestDto.setAuthKey(authKey);

        UserTb user = requestDto.toUserTb(passwordEncoder);
        return UserResponseDto.of(userRepository.save(user));
    }

    // 🔐로그인
    /* ✨예외 처리 ✨
        1. 사용자가 있는지 확인✅
        2. 아이디 비밀번호 맞는지 확인✅
        3. 이메일 인증 관련 IsActive 유무 확인✅
    */
    public TokenDto login(UserRequestDto requestDto) {
        UsernamePasswordAuthenticationToken authenticationToken = requestDto.toAuthentication();
        // 사용자가 있는지 확인
        UserTb loginUser = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다. 회원가입 진행 후 다시 시도해주세요."));

        // 비밀번호 맞는지 확인
        if (!passwordEncoder.matches(requestDto.getPwd(), loginUser.getPwd())) {
            throw new IllegalArgumentException("비밀번호가 맞지 않습니다.");
        }

        // 권한 확인
        if (loginUser.getAuthority().equals(Authority.ROLE_ADMIN)) {
            try {
                Authentication authentication = managerBuilder.getObject().authenticate(authenticationToken);
                return tokenProvider.generateTokenDto(authentication);
            } catch (AuthenticationException e) {
                throw e;
            }
        } else if (loginUser.getAuthority().equals(Authority.ROLE_USER)) {
            if(loginUser.getIsActive().equals(IsActive.ACTIVE)) {
                try {
                    Authentication authentication = managerBuilder.getObject().authenticate(authenticationToken);
                    return tokenProvider.generateTokenDto(authentication);
                } catch (AuthenticationException e) {
                    throw e;
                }
            } else {
                throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
            }
        } else {
            throw new IllegalArgumentException("권한이 올바르지 않습니다.");
        }
    }

    // 🔑토큰 검증 및 사용자 정보 추출
    public UserTb validateTokenAndGetUser(HttpServletRequest request, UserDetails userDetails) {
        // ♻️토큰 추출
        String accessToken = request.getHeader("Authorization");
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }
        // 🔑토큰 유효한지 검증
        if (accessToken != null && tokenProvider.validateToken(accessToken)) {
            Long userId = Long.valueOf(userDetails.getUsername());
            UserTb user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));
            return user;
        } else {
            throw new TokenExpiredException("🔴토큰이 만료됐습니다. Refresh Token을 보내주세요.");
        }
    }

    // 임시 비밀번호 발송 및 회원정보 업데이트
    public void updatePasswordWithAuthKey(String to) throws Exception {
        String ePw = emailService.sendPasswordAuthKey(to);
        UserTb user = userRepository.findByEmail(to)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 없습니다."));
        user.setPwd(ePw);
        userRepository.save(user);
    }
}
