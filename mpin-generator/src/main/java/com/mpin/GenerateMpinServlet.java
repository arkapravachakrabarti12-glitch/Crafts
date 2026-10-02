package com.mpin;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * POST /generateMpin  length=4|6, label=&lt;purpose&gt;
 *   -> {"mpin":"4829","length":4,"label":"Bank app","generatedAt":"..."}
 */
@WebServlet("/generateMpin")
public class GenerateMpinServlet extends HttpServlet {

    static final int MAX_LABEL_LENGTH = 60;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        int length;
        try {
            length = Integer.parseInt(req.getParameter("length"));
        } catch (NumberFormatException e) {
            length = -1;
        }
        if (!MpinGenerator.ALLOWED_LENGTHS.contains(length)) {
            Json.send(resp, HttpServletResponse.SC_BAD_REQUEST, Json.error("Length must be 4 or 6"));
            return;
        }

        String label = cleanLabel(req.getParameter("label"));
        if (label.isEmpty()) {
            Json.send(resp, HttpServletResponse.SC_BAD_REQUEST, Json.error("Please enter a label (what this MPIN is for)"));
            return;
        }
        if (label.length() > MAX_LABEL_LENGTH) {
            Json.send(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Json.error("Label must be at most " + MAX_LABEL_LENGTH + " characters"));
            return;
        }

        HttpSession session = req.getSession(false);
        MpinRecord record = new MpinRecord(
                MpinGenerator.generate(length),
                length,
                label,
                LocalDateTime.now(),
                (String) session.getAttribute("user"));
        MpinHistory.add(session, record);

        Json.send(resp, HttpServletResponse.SC_OK, Json.record(record));
    }

    /** Trims, drops control characters and collapses runs of whitespace. */
    static String cleanLabel(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
    }
}
