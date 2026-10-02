package com.mpin;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /history -> MPINs generated this session; DELETE /history clears them. */
@WebServlet("/history")
public class HistoryServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Json.send(resp, HttpServletResponse.SC_OK, Json.records(MpinHistory.snapshot(req.getSession(false))));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        MpinHistory.clear(req.getSession(false));
        Json.send(resp, HttpServletResponse.SC_OK, "[]");
    }
}
