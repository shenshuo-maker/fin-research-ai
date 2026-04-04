package com.finresearch.web;

import com.finresearch.common.ApiResponse;
import com.finresearch.domain.User;
import com.finresearch.dto.LoginRequest;
import com.finresearch.dto.RegisterRequest;
import com.finresearch.dto.TokenResponse;
import com.finresearch.repository.UserRepository;
import com.finresearch.security.JwtService;
import com.finresearch.security.SecurityUser;
import com.finresearch.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;

    @PostMapping("/register")
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User u = new User();
        u.setUsername(req.username());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setDisplayName(req.displayName() != null ? req.displayName() : req.username());
        u.setRole(User.Role.USER);
        userRepository.save(u);
        auditService.log(u.getId(), "REGISTER", "USER", u.getId(), null);
        String token = jwtService.createToken(u.getId(), u.getUsername());
        return ApiResponse.ok(new TokenResponse(token, u.getUsername(), u.getId()));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        SecurityUser su = (SecurityUser) auth.getPrincipal();
        auditService.log(su.getId(), "LOGIN", "USER", su.getId(), null);
        String token = jwtService.createToken(su.getId(), su.getUsername());
        return ApiResponse.ok(new TokenResponse(token, su.getUsername(), su.getId()));
    }
}
