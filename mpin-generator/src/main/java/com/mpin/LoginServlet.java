package com.mpin;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Credentials come from the MPIN_USERNAME / MPIN_PASSWORD environment
 * variables (defaults: admin / admin@123 — change them before deploying).
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String USERNAME = env("MPIN_USERNAME", "admin");
    private static final String PASSWORD = env("MPIN_PASSWORD", "admin@123");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String base = req.getContextPath();

        boolean userOk = matches(username, USERNAME);
        boolean passOk = matches(password, PASSWORD);   // always check both: no timing hint
        if (!(userOk && passOk)) {
            resp.sendRedirect(base + "/login.html?error=1");
            return;
        }

        // Fresh session on login to prevent session fixation.
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = req.getSession(true);
        session.setAttribute("user", USERNAME);
        session.setMaxInactiveInterval(15 * 60);

        resp.sendRedirect(base + "/index.html");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/login.html");
    }

    private static boolean matches(String given, String expected) {
        byte[] a = (given == null ? "" : given).getBytes(StandardCharsets.UTF_8);
        byte[] b = expected.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
