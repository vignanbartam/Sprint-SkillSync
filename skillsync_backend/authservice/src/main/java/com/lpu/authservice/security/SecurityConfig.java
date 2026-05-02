package com.lpu.authservice.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.lpu.authservice.entity.User;
import com.lpu.authservice.repository.UserRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Swagger UI — all variants
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/webjars/**",
                    "/swagger-resources/**"
                ).permitAll()
                // Public auth endpoints
                .requestMatchers(
                    "/auth/register",
                    "/auth/register/verify",
                    "/auth/login",
                    "/auth/internal/user/**",
                    "/auth/mentors",
                    "/auth/profile-picture/**",
                    "/auth/biodata/**"
                ).permitAll()
                // Role-based endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasRole("USER")
                // Everything else needs authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner initAdmin(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (!repo.existsByEmail("admin@gmail.com")) {
                User admin = User.builder()
                        .email("admin@gmail.com")
                        .password(encoder.encode("admin123"))
                        .name("System Admin")
                        .age(30)
                        .role("ROLE_ADMIN")
                        .mentorApproved(true)
                        .build();
                repo.save(admin);
                System.out.println("✅ Default Admin Created: admin@gmail.com / admin123");
            }
        };
    }
}
