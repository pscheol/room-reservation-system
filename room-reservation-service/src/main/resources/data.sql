-- ============================================
-- 회의실 예약 시스템 초기 데이터
-- ============================================

-- 회의실 데이터 (3개)
INSERT INTO tb_room (building_name, floor, room_name, capacity, contents, room_status, created_at, updated_at)
VALUES ('본사', 3, '회의실 A', 10, '프로젝터, 화이트보드, 대형 모니터 구비', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO tb_room (building_name, floor, room_name, capacity, contents, room_status, created_at, updated_at)
VALUES ('본사', 5, '회의실 B', 6, '소회의실, 화상회의 장비 구비', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO tb_room (building_name, floor, room_name, capacity, contents, room_status, created_at, updated_at)
VALUES ('본사', 10, '대회의실', 20, '대규모 회의 가능, 프레젠테이션 장비 완비', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);