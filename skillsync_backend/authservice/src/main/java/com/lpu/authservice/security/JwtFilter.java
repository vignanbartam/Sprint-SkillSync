package com.lpu.authservice.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.lpu.authservice.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        
        String path = request.getRequestURI();

        

     // Allow Swagger + explicitly public auth endpoints globally
        if (path.contains("swagger") ||
        	    path.contains("api-docs") ||
        	    path.contains("webjars") ||
        	    path.contains("swagger-resources") ||
        	    path.equals("/auth/register") ||
        	    path.equals("/auth/login") ||
        	    path.startsWith("/auth/internal/user/") ||
        	    path.equals("/auth/mentors") ||
        	    path.startsWith("/auth/profile-picture/") ||
        	    path.startsWith("/auth/biodata/")) {

        	    filterChain.doFilter(request, response);
        	    return;
        	}

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            String email = jwtUtil.extractEmail(token);

            String role = userRepo.findRoleByEmail(email).orElse(null);

            if (role != null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
