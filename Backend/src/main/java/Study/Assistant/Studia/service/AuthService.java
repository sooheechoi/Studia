package Study.Assistant.Studia.service;

import Study.Assistant.Studia.dto.auth.JwtAuthenticationResponse;
import Study.Assistant.Studia.dto.auth.LoginRequest;
import Study.Assistant.Studia.dto.auth.SignupRequest;
import Study.Assistant.Studia.domain.entity.User;
import Study.Assistant.Studia.domain.entity.User.UserRole;
import Study.Assistant.Studia.exception.DuplicateEmailException;
import Study.Assistant.Studia.exception.InvalidCredentialsException;
import Study.Assistant.Studia.repository.UserRepository;
import Study.Assistant.Studia.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    
    @Value("${jwt.expiration}")
    private int jwtExpirationInMs;
    
    @Transactional
    public JwtAuthenticationResponse signup(SignupRequest request) {
        try {
            // 이메일 중복 체크
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateEmailException("Email is already in use!");
            }
            
            // 필수 필드 검증
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Name is required");
            }
            if (request.getUniversity() == null || request.getUniversity().trim().isEmpty()) {
                throw new IllegalArgumentException("University is required");
            }
            if (request.getPassword() == null || request.getPassword().length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters long");
            }
            
            // 사용자 생성
            User user = User.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .name(request.getName())
                    .university(request.getUniversity())
                    .major(request.getMajor())
                    .grade(request.getGrade())
                    .role(UserRole.STUDENT)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            User savedUser = userRepository.save(user);
            log.info("New user registered: {}", savedUser.getEmail());
            
            // 자동 로그인
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            return createTokenResponse(authentication, savedUser);
        } catch (DuplicateEmailException e) {
            throw e; // 이미 처리된 예외는 그대로 전달
        } catch (Exception e) {
            log.error("Signup failed for email: {}", request.getEmail(), e);
            throw new RuntimeException("Failed to create account: " + e.getMessage(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public JwtAuthenticationResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
            
            return createTokenResponse(authentication, user);
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }
    
    public JwtAuthenticationResponse refreshToken(String refreshToken) {
        if (refreshToken != null && refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }
        
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }
        
        String username = tokenProvider.getUsernameFromJWT(refreshToken);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username, null, SecurityContextHolder.getContext().getAuthentication().getAuthorities()
        );
        
        return createTokenResponse(authentication, user);
    }
    
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 토큰을 블랙리스트에 추가 (Redis 사용시 주석 해제)
        String username = tokenProvider.getUsernameFromJWT(token);
        // redisTemplate.opsForValue().set(
        //         "blacklist_" + token, 
        //         username, 
        //         jwtExpirationInMs, 
        //         TimeUnit.MILLISECONDS
        // );
        
        log.info("User {} logged out", username);
        SecurityContextHolder.clearContext();
    }
    
    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        // Delete all related data first
        // This should be handled by cascade settings, but explicitly deleting for safety
        userRepository.delete(user);
        log.info("User account deleted: {}", email);
    }
    
    private JwtAuthenticationResponse createTokenResponse(Authentication authentication, User user) {
        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);
        
        return JwtAuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn((long) jwtExpirationInMs)
                .user(JwtAuthenticationResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .university(user.getUniversity())
                        .major(user.getMajor())
                        .grade(user.getGrade())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}
