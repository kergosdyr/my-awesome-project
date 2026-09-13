CREATE TABLE event (event_id BIGINT PRIMARY KEY, remaining INT NOT NULL CHECK (remaining >= 0));
CREATE TABLE reservation (reservation_id BIGINT AUTO_INCREMENT PRIMARY KEY, event_id BIGINT NOT NULL REFERENCES event(event_id), user_id BIGINT NOT NULL);
