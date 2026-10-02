package com.mpin;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;

/** Per-session list of generated MPINs, newest last. */
final class MpinHistory {

    private static final String ATTR = "mpinHistory";
    static final int MAX_ENTRIES = 500;

    private MpinHistory() {
    }

    @SuppressWarnings("unchecked")
    static List<MpinRecord> of(HttpSession session) {
        synchronized (session) {
            List<MpinRecord> list = (List<MpinRecord>) session.getAttribute(ATTR);
            if (list == null) {
                list = new ArrayList<>();
                session.setAttribute(ATTR, list);
            }
            return list;
        }
    }

    static void add(HttpSession session, MpinRecord record) {
        List<MpinRecord> list = of(session);
        synchronized (list) {
            list.add(record);
            if (list.size() > MAX_ENTRIES) {
                list.remove(0);
            }
        }
    }

    static List<MpinRecord> snapshot(HttpSession session) {
        List<MpinRecord> list = of(session);
        synchronized (list) {
            return List.copyOf(list);
        }
    }

    static void clear(HttpSession session) {
        List<MpinRecord> list = of(session);
        synchronized (list) {
            list.clear();
        }
    }
}
