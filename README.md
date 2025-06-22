프로젝트의 핵심 목표는 일정(Schedule) 데이터를 MySQL 데이터베이스와 연동하고, FastAPI를 통해 이를 등록 및 조회할 수 있는 REST API를 구현하는 것입니다.

[실행 방법]

1. MySQL 데이터베이스 생성
   
MySQL 서버가 설치되어 있어야 하며, 아래 쿼리를 먼저 실행합니다.
  
CREATE DATABASE studia_db;
USE studia_db;
CREATE TABLE schedule (
id INT AUTO_INCREMENT PRIMARY KEY,
title VARCHAR(100),
type ENUM('class', 'exam'),
date DATE,
time TIME
);

2. FastAPI 서버 실행
   
1) 터미널에서 schedule_api 디렉토리로 이동
2) 아래 명령어를 순서대로 실행
   
pip install -r requirements.txt
uvicorn main:app --reload

3. Swagger 문서 접속

브라우저에서 아래 주소로 접속
  
http://localhost:8000/docs
  
[테스트 예시]
  
Swagger에서 POST /schedule/add 를 선택한 뒤 아래 데이터를 입력하고 실행합니다.
  
{
"title": "데이터베이스",
"type": "class",
"date": "2025-06-26",
"time": "14:00:00"
}
  
성공하면 "Schedule added successfully"라는 메시지가 뜹니다.
그 후 GET /schedule/list 를 실행하면 등록한 일정이 조회됩니다.

[기타 사항]

본 저장소는 실제 운영 배포용이 아닌, 프로젝트 제출용으로 작성되었습니다.
교수님께서 직접 실행이 어려우실 경우를 대비하여, 보고서 및 발표자료에 실행화면 스크린샷을 첨부하  였습니다.
