-- 초기 스키마입니다. 요구사항에 필요한 제약/인덱스는 직접 추가하세요.
CREATE TABLE event (
    event_id BIGINT PRIMARY KEY,
    remaining INT NOT NULL,
    version INT NOT NULL DEFAULT 0
);
CREATE TABLE reservation (
    reservation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL
);
