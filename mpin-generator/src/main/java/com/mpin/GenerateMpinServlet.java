package com.mpin;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;

/** POST /generateMpin?length=4|6 -> {"mpin":"4829","length":4,"generatedAt":"..."} */
@WebServlet("/generateMpin")
public class GenerateMpinServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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

        HttpSession session = req.getSession(false);
        MpinRecord record = new MpinRecord(
                MpinGenerator.generate(length),
                length,
                LocalDateTime.now(),
                (String) session.getAttribute("user"));
        MpinHistory.add(session, record);

        Json.send(resp, HttpServletResponse.SC_OK, Json.record(record));
    }
}
