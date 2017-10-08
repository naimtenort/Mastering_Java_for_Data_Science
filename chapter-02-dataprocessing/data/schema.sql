-- 기본 사용 스키마 생성
CREATE SCHEMA people DEFAULT CHARACTER SET utf8 ;
CREATE TABLE people.people (
	person_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
	name VARCHAR(45) NULL,
	email VARCHAR(100) NULL,
	country VARCHAR(45) NULL,
	salary INT NULL,
	experience INT NULL,
	PRIMARY KEY (person_id));