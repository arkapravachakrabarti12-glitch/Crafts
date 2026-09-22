-- TeachNet initial schema (MySQL 8)

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(190) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE teacher_profiles (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    headline         VARCHAR(200),
    bio              TEXT,
    city             VARCHAR(100),
    state            VARCHAR(100),
    years_experience INT          NOT NULL DEFAULT 0,
    photo_url        VARCHAR(500),
    open_to_work     BOOLEAN      NOT NULL DEFAULT TRUE,
    verified         BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at       DATETIME(6)  NOT NULL,
    CONSTRAINT uq_teacher_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_teacher_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_teacher_profiles_city ON teacher_profiles (city);

CREATE TABLE teacher_subjects (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    profile_id BIGINT      NOT NULL,
    subject    VARCHAR(100) NOT NULL,
    board      VARCHAR(30)  NOT NULL,
    grade_from INT          NOT NULL,
    grade_to   INT          NOT NULL,
    CONSTRAINT fk_teacher_subjects_profile FOREIGN KEY (profile_id) REFERENCES teacher_profiles (id) ON DELETE CASCADE
);
CREATE INDEX idx_teacher_subjects_subject ON teacher_subjects (subject);

CREATE TABLE qualifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    profile_id      BIGINT       NOT NULL,
    degree          VARCHAR(150) NOT NULL,
    institute       VARCHAR(200) NOT NULL,
    completion_year INT,
    verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_qualifications_profile FOREIGN KEY (profile_id) REFERENCES teacher_profiles (id) ON DELETE CASCADE
);

CREATE TABLE experiences (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    profile_id   BIGINT       NOT NULL,
    title        VARCHAR(150) NOT NULL,
    organization VARCHAR(200) NOT NULL,
    start_year   INT          NOT NULL,
    end_year     INT,
    description  TEXT,
    CONSTRAINT fk_experiences_profile FOREIGN KEY (profile_id) REFERENCES teacher_profiles (id) ON DELETE CASCADE
);

CREATE TABLE portfolio_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    profile_id  BIGINT       NOT NULL,
    item_type   VARCHAR(30)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    url         VARCHAR(500) NOT NULL,
    description TEXT,
    CONSTRAINT fk_portfolio_items_profile FOREIGN KEY (profile_id) REFERENCES teacher_profiles (id) ON DELETE CASCADE
);

CREATE TABLE institutions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id        BIGINT       NOT NULL,
    name            VARCHAR(200) NOT NULL,
    institution_type VARCHAR(30) NOT NULL,
    board           VARCHAR(30),
    city            VARCHAR(100),
    state           VARCHAR(100),
    about           TEXT,
    website         VARCHAR(300),
    logo_url        VARCHAR(500),
    verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT uq_institutions_owner UNIQUE (owner_id),
    CONSTRAINT fk_institutions_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_institutions_city ON institutions (city);

CREATE TABLE connections (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    requester_id BIGINT      NOT NULL,
    addressee_id BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    CONSTRAINT uq_connections_pair UNIQUE (requester_id, addressee_id),
    CONSTRAINT fk_connections_requester FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_connections_addressee FOREIGN KEY (addressee_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_connections_addressee ON connections (addressee_id, status);

CREATE TABLE follows (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id    BIGINT      NOT NULL,
    institution_id BIGINT      NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    CONSTRAINT uq_follows UNIQUE (follower_id, institution_id),
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_institution FOREIGN KEY (institution_id) REFERENCES institutions (id) ON DELETE CASCADE
);

CREATE TABLE posts (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    author_id     BIGINT      NOT NULL,
    content       TEXT        NOT NULL,
    image_url     VARCHAR(500),
    like_count    INT         NOT NULL DEFAULT 0,
    comment_count INT         NOT NULL DEFAULT 0,
    created_at    DATETIME(6) NOT NULL,
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_posts_author_created ON posts (author_id, created_at);
CREATE INDEX idx_posts_created ON posts (created_at);

CREATE TABLE post_likes (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uq_post_likes UNIQUE (post_id, user_id),
    CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE comments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT      NOT NULL,
    author_id  BIGINT      NOT NULL,
    content    TEXT        NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_comments_post ON comments (post_id, created_at);

CREATE TABLE job_openings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id  BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT         NOT NULL,
    subject         VARCHAR(100) NOT NULL,
    board           VARCHAR(30),
    grade_from      INT,
    grade_to        INT,
    employment_type VARCHAR(30)  NOT NULL,
    salary_min      INT,
    salary_max      INT,
    city            VARCHAR(100),
    status          VARCHAR(20)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_job_openings_institution FOREIGN KEY (institution_id) REFERENCES institutions (id) ON DELETE CASCADE
);
CREATE INDEX idx_job_openings_status_created ON job_openings (status, created_at);
CREATE INDEX idx_job_openings_subject ON job_openings (subject);

CREATE TABLE job_applications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id     BIGINT      NOT NULL,
    teacher_id BIGINT      NOT NULL,
    cover_note TEXT,
    status     VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uq_job_applications UNIQUE (job_id, teacher_id),
    CONSTRAINT fk_job_applications_job FOREIGN KEY (job_id) REFERENCES job_openings (id) ON DELETE CASCADE,
    CONSTRAINT fk_job_applications_teacher FOREIGN KEY (teacher_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE conversations (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_a_id  BIGINT      NOT NULL,
    user_b_id  BIGINT      NOT NULL,
    last_message VARCHAR(300),
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uq_conversations_pair UNIQUE (user_a_id, user_b_id),
    CONSTRAINT fk_conversations_a FOREIGN KEY (user_a_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversations_b FOREIGN KEY (user_b_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE messages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT      NOT NULL,
    sender_id       BIGINT      NOT NULL,
    body            TEXT        NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    read_at         DATETIME(6),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_messages_conversation ON messages (conversation_id, created_at);

CREATE TABLE notifications (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    notification_type VARCHAR(40)  NOT NULL,
    message           VARCHAR(500) NOT NULL,
    link              VARCHAR(300),
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        DATETIME(6)  NOT NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_notifications_user ON notifications (user_id, is_read, created_at);
