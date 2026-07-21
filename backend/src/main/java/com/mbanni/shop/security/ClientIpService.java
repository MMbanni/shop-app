package com.mbanni.shop.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class ClientIpService {

    public String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}