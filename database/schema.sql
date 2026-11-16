-- ============================================================================
-- Job Application Tracker - MySQL 8 schema
-- Run once on an empty database. With Docker Compose this file is executed
-- automatically on first start (mounted into /docker-entrypoint-initdb.d).
-- ============================================================================

CREATE DATABASE IF NOT EXISTS job_tracker
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE job_tracker;

-- ----------------------------------------------------------------------------
-- users
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,            -- BCrypt hash (60 chars), never plaintext
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- job_applications  (one user -> many applications)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS job_applications (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    company_name     VARCHAR(150) NOT NULL,
    job_title        VARCHAR(150) NOT NULL,
    job_description  TEXT         NULL,
    location         VARCHAR(150) NULL,
    job_type         VARCHAR(20)  NOT NULL,
    application_date DATE         NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    job_url          VARCHAR(500) NULL,
    salary_range     VARCHAR(100) NULL,
    recruiter_name   VARCHAR(100) NULL,
    recruiter_email  VARCHAR(255) NULL,
    notes            TEXT         NULL,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_applications_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_applications_job_type
        CHECK (job_type IN ('FULL_TIME', 'PART_TIME', 'INTERNSHIP')),
    CONSTRAINT chk_applications_status
        CHECK (status IN ('APPLIED', 'ASSESSMENT', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN')),
    -- Every list/dashboard query is scoped by user_id first.
    INDEX idx_applications_user_date   (user_id, application_date),
    INDEX idx_applications_user_status (user_id, status),
    INDEX idx_applications_user_type   (user_id, job_type)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- interviews  (one application -> many interviews)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS interviews (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    application_id   BIGINT       NOT NULL,
    round_name       VARCHAR(100) NOT NULL,
    interview_type   VARCHAR(20)  NOT NULL,
    scheduled_at     DATETIME(6)  NOT NULL,         -- local date/time as entered (no time zone)
    location_or_link VARCHAR(500) NULL,
    notes            TEXT         NULL,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_interviews_application
        FOREIGN KEY (application_id) REFERENCES job_applications (id) ON DELETE CASCADE,
    CONSTRAINT chk_interviews_type
        CHECK (interview_type IN ('ONLINE', 'PHONE', 'IN_PERSON')),
    INDEX idx_interviews_application_time (application_id, scheduled_at),
    INDEX idx_interviews_scheduled_at     (scheduled_at)
) ENGINE = InnoDB;

-- ----------------------------------------------------------------------------
-- application_notes  (one application -> many notes)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS application_notes (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    application_id BIGINT      NOT NULL,
    category       VARCHAR(30) NOT NULL,
    content        TEXT        NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_notes_application
        FOREIGN KEY (application_id) REFERENCES job_applications (id) ON DELETE CASCADE,
    CONSTRAINT chk_notes_category
        CHECK (category IN ('RECRUITER_CONVERSATION', 'INTERVIEW_FEEDBACK', 'FOLLOW_UP', 'PREPARATION', 'GENERAL')),
    INDEX idx_notes_application_created (application_id, created_at)
) ENGINE = InnoDB;
