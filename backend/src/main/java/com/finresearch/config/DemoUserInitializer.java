package com.finresearch.config;

import com.finresearch.domain.User;
import com.finresearch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DemoUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("demo")) {
            User u = new User();
            u.setUsername("demo");
            u.setPasswordHash(passwordEncoder.encode("demo123456"));
            u.setDisplayName("演示账号");
            u.setRole(User.Role.USER);
            userRepository.save(u);
        }
    }
}
