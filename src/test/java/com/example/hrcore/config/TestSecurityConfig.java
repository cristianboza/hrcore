package com.example.hrcore.config;

import com.example.hrcore.entity.User;
import com.example.hrcore.repository.UserRepository;
import com.example.hrcore.service.AuthenticationService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("test")
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        
        return http.build();
    }

    @Bean
    @Primary
    public AuthenticationService testAuthenticationService(UserRepository userRepository) {
        return new AuthenticationService(null, userRepository) {
            @Override
            public User getCurrentUser(Authentication authentication) {
                if (authentication == null || authentication.getName() == null) {
                    throw new IllegalStateException("No authentication found");
                }
                
                // Use the username (email) from mock authentication
                String email = authentication.getName();
                return userRepository.findByEmail(email)
                        .orElseThrow(() -> new IllegalStateException("User not found: " + email));
            }
        };
    }
}
