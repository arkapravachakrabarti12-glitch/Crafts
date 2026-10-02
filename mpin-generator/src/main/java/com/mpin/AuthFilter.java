package com.mpin;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter({"/", "/index.html", "/generateMpin", "/history", "/downloadExcel"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        // Never let the browser cache protected pages (back button after logout).
        resp.setHeader("Cache-Control", "no-store");

        if (session == null || session.getAttribute("user") == null) {
            String path = req.getServletPath();
            if (path.equals("/generateMpin") || path.equals("/history")) {
                Json.send(resp, HttpServletResponse.SC_UNAUTHORIZED, Json.error("Session expired"));
            } else {
                resp.sendRedirect(req.getContextPath() + "/login.html");
            }
            return;
        }

        chain.doFilter(request, response);
    }
}
