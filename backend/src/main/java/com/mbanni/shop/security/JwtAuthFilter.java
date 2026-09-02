package com.mbanni.shop.security;

import com.mbanni.shop.user.User;
import com.mbanni.shop.user.UserRepository;
import com.mbanni.shop.user.UserStatus;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository, SecurityErrorResponseWriter securityErrorResponseWriter){
        this.jwtService = jwtService;
        this.userRepository=userRepository;
        this.securityErrorResponseWriter=securityErrorResponseWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Check header for authorization
        String authHeader = request.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token from header
        String jwt = authHeader.substring(7);
        JwtClaimsDTO userData;

        // Validate token
        try {
            userData = jwtService.extractUserData(jwt);
        } catch (IllegalArgumentException | JwtException e) {
            filterChain.doFilter(request, response);
            return;
        }
        User user = userRepository.findById(userData.id()).orElse(null);
        if(user!=null){
            if(user.getStatus() == UserStatus.SUSPENDED){
                securityErrorResponseWriter.writeSuspended(response, user.getSuspendedUntil());

            }
            if(user.getStatus() == UserStatus.BANNED){
                securityErrorResponseWriter.writeBanned(response);

            }
        }



        if (user == null) {
            filterChain.doFilter(request,response);
        }


        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + userData.role())
        );

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userData.id(),
                null,
                authorities


        );

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);

    }



}
