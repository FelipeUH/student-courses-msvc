CREATE TABLE students (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(255),
    last_name VARCHAR(255),
    email     VARCHAR(255),
    course_id BIGINT
);
