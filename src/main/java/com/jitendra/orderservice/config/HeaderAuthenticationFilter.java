package com.jitendra.orderservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        System.out.println("path->"+request.getRequestURI());
        String email = request.getHeader("X-User-Email");
        String rolesHeader = request.getHeader("X-User-Roles");
        String userId=      request.getHeader("X-User-Id");
        System.out.println("rolesHeader->"+rolesHeader);
        System.out.println("email->"+email);
        System.out.println("userId->"+userId);
        if (email != null && rolesHeader != null) {

            List<GrantedAuthority> authorities =
                    Arrays.stream(rolesHeader.split(","))
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());
            System.out.println("authorities->"+authorities.get(1));
            UserPrincipal principal = new UserPrincipal(userId, email);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            authorities
                    );

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}