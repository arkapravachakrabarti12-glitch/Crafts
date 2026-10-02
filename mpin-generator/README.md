# MPIN Generator

A small Jakarta Servlet web app that generates secure **4-digit or 6-digit MPINs** (the user picks the length), each tagged with a **label** saying what it's for. It keeps a history for the session and exports it to Excel.

## Features

- **Login-protected.** Session-based auth with a fresh session on login (prevents session fixation), a 15-minute idle timeout and HttpOnly cookies.
- **Secure generation.** Digits come from `SecureRandom`. Only lengths 4 and 6 are accepted, and the server enforces this as well as the UI.
- **No weak MPINs.** It never issues all-same digits (`1111`), sequences (`1234`, `4321`, `7890`), repeating blocks (`1212`, `123123`), PINs with 2 or fewer distinct digits (`1122`), or common PINs (`2580`, `1004`...).
- **Modern UI.** Responsive dark glass design, a slot-machine style digit reveal, copy-to-clipboard, show/hide, keyboard shortcuts (`4`/`6`, `Enter`, `C`, `H`) and reduced-motion support.
- **Labels.** Every MPIN needs a label / purpose (up to 60 characters, e.g. "SBI mobile banking"). The label is shown on screen and in the history, recent labels are suggested as you type, and it's saved in its own column in the Excel file.
- **Excel export.** The `.xlsx` is built in memory with Apache POI from the current session's MPINs. MPINs are stored as text, so leading zeros are kept. No hard-coded `C:/` path.

## Run

Requires JDK 17+ and Maven.

```bash
cd mpin-generator
mvn jetty:run          # http://localhost:8080/
```

Default login: **admin / admin@123**. Change it with environment variables before you deploy:

```bash
MPIN_USERNAME=alice MPIN_PASSWORD='s3cret!' mvn jetty:run
```

To deploy on Tomcat 10.1+ (or any Jakarta EE 10 container), run `mvn package` and drop `target/mpin-generator.war` into `webapps/`.

## Endpoints

| Method | Path             | Description                                     |
| ------ | ---------------- | ----------------------------------------------- |
| POST   | `/login`         | Form login → redirects to `index.html`          |
| GET    | `/logout`        | Ends the session                                |
| POST   | `/generateMpin`  | `length=4\|6`, `label=...` → `{"mpin","length","label","generatedAt"}` |
| GET    | `/history`       | MPINs generated this session (JSON)             |
| DELETE | `/history`       | Clears this session's history                   |
| GET    | `/downloadExcel` | Downloads `mpin_data_<timestamp>.xlsx`          |

`AuthFilter` protects everything except the login page and static assets. JSON endpoints return `401` instead of a redirect.

## Layout

```
src/main/java/com/mpin/     servlets, filter, MpinGenerator (core logic)
src/main/webapp/            login.html, index.html, css/, js/
src/test/java/com/mpin/     unit tests for the generator
```
