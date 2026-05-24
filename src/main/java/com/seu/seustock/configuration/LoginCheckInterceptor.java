package com.seu.seustock.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loginUser") == null) {
            String requestURI = request.getRequestURI();
            String queryString = request.getQueryString();
            String fullPath = (queryString != null) ? requestURI + "?" + queryString : requestURI;
            response.sendRedirect("/login?redirect=" + fullPath);
            return false;
        }
        return true;
    }
}
