-- Testcontainers PostgreSQL 초기화 스크립트
-- Hibernate 가 못 만드는 부분만 (enum 타입, CAST, extension) — 테이블은 ddl-auto=update 가 알아서 생성.

-- ltree extension (m_expense_category 등에서 사용)
CREATE EXTENSION IF NOT EXISTS ltree;

CREATE TYPE day_type AS ENUM ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN');
CREATE TYPE rsv_status AS ENUM ('RESERVED', 'COMPLETED', 'CANCELED');
CREATE TYPE order_status AS ENUM ('READY', 'COOKED', 'PAID');
CREATE TYPE pay_type AS ENUM ('CASH', 'CARD');

-- varchar[] -> day_type[] 자동 cast (Hibernate 가 String[] 로 보내는 것 받기 위함)
CREATE CAST (varchar[] AS day_type[]) WITH INOUT AS IMPLICIT;
-- scalar varchar -> day_type (array_position 등 함수 호출 시 element 타입 호환 위함)
CREATE CAST (varchar AS day_type) WITH INOUT AS IMPLICIT;

-- array_position(day_type[], varchar) overload — PG polymorphic resolver 가 implicit cast
-- 까지 추적하지 못해 함수 자체를 못 찾는 문제 회피.
CREATE OR REPLACE FUNCTION array_position(day_type[], varchar)
RETURNS integer AS $$
    SELECT array_position($1, $2::day_type);
$$ LANGUAGE sql IMMUTABLE;
