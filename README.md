# TeachNet — a professional network for teachers

A LinkedIn-style web portal built around teachers instead of generic jobs. Teachers build a teaching profile and portfolio, connect with other educators, share posts and apply to teaching jobs. Schools, coaching centres and colleges get an institution page, post openings and manage applicants.

| Layer    | Tech                                                                  |
| -------- | --------------------------------------------------------------------- |
| Frontend | React 18 + TypeScript, Vite, Tailwind CSS, TanStack Query, React Router |
| Backend  | Java 21, Spring Boot 3 (Web, Security + JWT, Data JPA), Flyway        |
| Database | MySQL 8                                                               |
| Hosting  | Frontend → Vercel · Backend → Render (Docker) · MySQL → any MySQL host |

## Features

**Teachers**
- Profile: headline, bio, city, experience, "open to work", photo
- Subjects taught, with board (CBSE / ICSE / IB / IGCSE / State…) and grade range
- Experience, education and certifications (B.Ed., CTET…)
- Teaching portfolio: demo-lesson videos, lesson plans, results, certificates (as links)
- Connections: request, accept or ignore, withdraw; "teachers you may know"
- Job search with filters, recommended jobs, one-click apply with cover note, application tracking

**Institutions**
- Institution page (type, board, city, about, website, logo) that teachers can follow
- Post, edit, close and reopen jobs; see applicants' profiles; shortlist, reject or hire

**Everyone**
- Feed (your network, or everyone), posts with images, likes and comments
- 1:1 messaging with unread counts
- Notifications (connection requests, likes, comments, applications, status changes, verification)
- Search for teachers and institutions
- Public job board (browsable without logging in)

**Admin**
- Stats, verify teachers and institutions (verified badge), disable accounts

## Project structure

```
Crafts/
├── backend/                     Spring Boot API
│   ├── src/main/java/com/teachnet/
│   │   ├── auth/                register, login, /me (JWT)
│   │   ├── security/            JWT filter, Spring Security config, CORS
│   │   ├── user/                users, roles, user summaries
│   │   ├── profile/             teacher profiles, subjects, qualifications, experience, portfolio, search
│   │   ├── institution/         institution pages, follow
│   │   ├── network/             connections, follows
│   │   ├── feed/                posts, likes, comments
│   │   ├── jobs/                job openings, applications, recommendations
│   │   ├── messaging/           conversations, messages
│   │   ├── notification/        in-app notifications
│   │   ├── admin/               verification, moderation, stats
│   │   ├── seed/                admin bootstrap + optional demo data
│   │   └── common/              errors, paging, health
│   ├── src/main/resources/
│   │   ├── application.yml      all settings, overridable by env vars
│   │   └── db/migration/        Flyway SQL migrations (the MySQL schema)
│   ├── Dockerfile               used by Render
│   └── mvnw                     Maven wrapper (no Maven install needed)
├── frontend/                    React app
│   ├── src/api/                 axios client + TypeScript types for the API
│   ├── src/auth/                auth context (JWT in localStorage)
│   ├── src/components/          layout, post card, job/teacher cards, UI primitives
│   ├── src/pages/               one file per screen
│   └── vercel.json              SPA routing on Vercel
├── docker-compose.yml           local MySQL (optional)
└── render.yaml                  Render blueprint for the backend
```

The backend is a **modular monolith**: one deployable app, split into feature packages. Each package owns its entities, repository, service and controller, so any module (e.g. messaging) can later be split into its own service without a rewrite.

---

## Run locally

### Prerequisites
- **Java 21** (e.g. [Temurin](https://adoptium.net))
- **Node.js 20+**
- **MySQL 8**: either installed locally, or via Docker (`docker compose up -d mysql`)

### 1. Database

**Option A – Docker**
```bash
docker compose up -d mysql
```
This creates database `teachnet` with user `teachnet` and password `teachnet`, which the backend uses by default.

**Option B – MySQL installed on your machine**
```sql
CREATE DATABASE teachnet CHARACTER SET utf8mb4;
CREATE USER 'teachnet'@'localhost' IDENTIFIED BY 'teachnet';
GRANT ALL ON teachnet.* TO 'teachnet'@'localhost';
```

### 2. Backend (http://localhost:8080)
```bash
cd backend
# macOS / Linux
SEED_DEMO_DATA=true ADMIN_EMAIL=admin@teachnet.local ADMIN_PASSWORD=admin12345 ./mvnw spring-boot:run
# Windows (PowerShell)
$env:SEED_DEMO_DATA="true"; $env:ADMIN_EMAIL="admin@teachnet.local"; $env:ADMIN_PASSWORD="admin12345"; .\mvnw.cmd spring-boot:run
```
On first start, Flyway creates all the tables. With `SEED_DEMO_DATA=true`, an empty database is filled with sample teachers, schools, jobs and posts.

### 3. Frontend (http://localhost:5173)
```bash
cd frontend
npm install
npm run dev
```
In development, Vite forwards `/api` requests to `localhost:8080`, so no extra configuration is needed.

### Demo accounts (when `SEED_DEMO_DATA=true`)
| Role        | Email                              | Password      |
| ----------- | ---------------------------------- | ------------- |
| Teacher     | `priya@demo.teachnet.in`           | `password123` |
| Teacher     | `rahul@demo.teachnet.in`           | `password123` |
| School      | `hr@greenfield.demo.teachnet.in`   | `password123` |
| Coaching    | `jobs@brightpath.demo.teachnet.in` | `password123` |
| Admin       | whatever you set in `ADMIN_EMAIL`  | `ADMIN_PASSWORD` |

More demo teachers: `ananya@`, `arjun@`, `fatima@`, `meera@` (all `@demo.teachnet.in`).

### Tests
```bash
cd backend && ./mvnw test      # end-to-end API flow test against an in-memory DB
cd frontend && npm run build   # type-check + production build
```

---

## Deploy (free tiers): MySQL → Render → Vercel

### 1. MySQL database
Render doesn't offer managed MySQL, so use a MySQL host. Any of these work:
- **[Aiven for MySQL](https://aiven.io/mysql)** (has a free plan): real MySQL 8. Copy host, port, user and password from the console.
- **Railway**, **DigitalOcean**, **AWS RDS**, etc. (paid)

Build the JDBC URL from those details. Aiven requires SSL:
```
jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED&serverTimezone=UTC
```

### 2. Backend on Render
1. Push this repo to GitHub.
2. In Render: **New + → Blueprint**, select the repo. It reads `render.yaml`. (Or create a **Web Service** manually: Runtime *Docker*, Root directory `backend`.)
3. Fill in the environment variables:

| Variable               | Value |
| ---------------------- | ----- |
| `DATABASE_URL`         | JDBC URL from step 1 |
| `DATABASE_USERNAME`    | DB user |
| `DATABASE_PASSWORD`    | DB password |
| `JWT_SECRET`           | auto-generated by the blueprint (or `openssl rand -base64 48`) |
| `CORS_ALLOWED_ORIGINS` | your Vercel URL, e.g. `https://teachnet.vercel.app,https://*.vercel.app` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | your admin login |
| `SEED_DEMO_DATA`       | `true` once, to get sample data (optional) |

4. Deploy, then check `https://<your-service>.onrender.com/api/health` returns `{"status":"ok"}`.

> Render's free plan sleeps after about 15 minutes of inactivity, so the first request after that takes around a minute. Upgrade the plan to avoid this.

### 3. Frontend on Vercel
1. In Vercel: **Add New → Project**, import the repo.
2. Set **Root Directory** to `frontend`. Vercel detects Vite automatically.
3. Add the environment variable `VITE_API_URL` = `https://<your-service>.onrender.com`.
4. Deploy. Then make sure the Vercel URL is in the backend's `CORS_ALLOWED_ORIGINS`.

---

## API overview

All endpoints are under `/api`. Send `Authorization: Bearer <token>` except where marked public.

| Area          | Endpoints |
| ------------- | --------- |
| Auth          | `POST /auth/register` · `POST /auth/login` (public) · `GET /auth/me` |
| Profiles      | `GET /profiles/{userId}` · `PUT /profiles/me` · `POST/DELETE /profiles/me/{subjects,qualifications,experiences,portfolio}` |
| Search        | `GET /teachers?q&city&subject&board&openToWork&minExperience` · `GET /institutions?q&city&type` |
| Institutions  | `GET /institutions/{id}` · `GET/PUT /institutions/me` · `POST/DELETE /institutions/{id}/follow` |
| Network       | `GET /connections` · `GET /connections/pending` · `GET /connections/suggestions` · `POST /connections/request/{userId}` · `POST /connections/{id}/accept` · `DELETE /connections/{id}` |
| Feed          | `GET /posts/feed?scope=NETWORK\|ALL` · `GET /posts/user/{id}` · `POST /posts` · `DELETE /posts/{id}` · `POST/DELETE /posts/{id}/like` · `GET/POST /posts/{id}/comments` |
| Jobs          | `GET /jobs` · `GET /jobs/{id}` (public) · `POST /jobs` · `PUT /jobs/{id}` · `PUT /jobs/{id}/status` · `GET /jobs/mine` · `GET /jobs/recommended` · `POST /jobs/{id}/apply` · `GET /jobs/{id}/applications` |
| Applications  | `GET /applications/mine` · `PUT /applications/{id}/status` · `DELETE /applications/{id}` |
| Messaging     | `GET /conversations` · `POST /conversations/with/{userId}` · `GET/POST /conversations/{id}/messages` · `GET /conversations/unread-count` |
| Notifications | `GET /notifications` · `GET /notifications/unread-count` · `POST /notifications/{id}/read` · `POST /notifications/read-all` |
| Admin         | `GET /admin/stats` · `GET /admin/teachers` · `GET /admin/institutions` · `POST /admin/teachers/{id}/verify` · `POST /admin/institutions/{id}/verify` · `POST /admin/users/{id}/enabled` |

## Known limitations and next steps
- **File uploads**: photos, logos and portfolio items are URLs for now. Next step: upload to S3 / Cloudflare R2 / Cloudinary.
- **Realtime**: messages and notification counts refresh by polling (every 5–30 seconds). Next step: WebSockets (Spring STOMP).
- **Search** uses SQL `LIKE` filters, which is fine for thousands of profiles. Next step: MySQL FULLTEXT indexes or OpenSearch.
- **Auth**: the JWT is kept in `localStorage` and stays valid for 7 days. A disabled account can't log in again, but a token it already has keeps working until it expires. Next steps: refresh tokens, email verification, password reset, Google / OTP login.
- **SEO**: the app renders in the browser. For search-engine-friendly public profiles and job pages, consider Next.js or pre-rendering.
