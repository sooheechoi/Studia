package Study.Assistant.Studia.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${openai.api.key:}")
    private String openAiApiKey;
    
    @Value("${claude.api.key:}")
    private String claudeApiKey;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiUrl;
    
    @Value("${claude.api.url:https://api.anthropic.com/v1/messages}")
    private String claudeUrl;
    
    @Value("${ai.model:openai}")
    private String preferredModel;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("=== AI Service Configuration ===");
        log.info("Preferred AI Model: {}", preferredModel);
        log.info("OpenAI API Key configured: {}", !openAiApiKey.isEmpty() && !openAiApiKey.equals("test-key"));
        log.info("Claude API Key configured: {}", !claudeApiKey.isEmpty() && !claudeApiKey.equals("test-key"));
        
        if (!openAiApiKey.isEmpty() && !openAiApiKey.equals("test-key")) {
            log.info("OpenAI API Key length: {}", openAiApiKey.length());
            log.info("OpenAI API Key starts with: {}", openAiApiKey.substring(0, Math.min(10, openAiApiKey.length())) + "...");
        }
        
        if (!claudeApiKey.isEmpty() && !claudeApiKey.equals("test-key")) {
            log.info("Claude API Key length: {}", claudeApiKey.length());
        }
        
        log.info("OpenAI API URL: {}", openAiUrl);
        log.info("Claude API URL: {}", claudeUrl);
        log.info("=================================");
    }
    
    public String generateSummary(String content) {
        // 컨텐츠 길이 제한 없음 - 전체 내용 사용
        String contentToSummarize = content;
        
        // 경고만 표시
        if (content.length() > 10000) {
            log.warn("Large content for summary: {} chars. This may take longer to process.", content.length());
        }
            
        String prompt = """
            대학생을 위한 학습 자료 요약을 작성해주세요.
            
            요약 형식:
            ## 📚 핵심 개념 요약
            
            ### 주요 내용
            - 이 자료의 핵심 주제와 개념을 3-5개 항목으로 정리
            - 각 항목은 간결하고 명확하게 설명
            
            ### 중요 포인트
            - 시험에 나올 만한 중요한 내용 3-4개
            - **굵은 글씨**로 핵심 용어 강조
            
            ### 학습 도움말
            - 이 내용을 공부할 때 유용한 팁 2-3개
            - 관련 개념이나 추가 학습이 필요한 부분
            
            내용:
            %s
            """.formatted(contentToSummarize);
        
        return callAI(prompt, "summary");
    }
    
    public List<String> extractKeyPoints(String content) {
        String prompt = """
            당신은 학습 내용의 핵심을 파악하는 전문가입니다.
            다음 내용에서 시험에 나올 가능성이 높은 핵심 포인트를 추출해주세요.
            
            추출 기준:
            1. 정의나 개념 설명 (시험의 30-40%%)
            2. 중요한 공식이나 원리 (시험의 20-30%%)
            3. 인과관계나 프로세스 (시험의 20%%)
            4. 비교/대조되는 내용 (시험의 15%%)
            5. 실제 적용 예시와 문제 해결 (시험의 15%%)
            
            각 포인트는 다음 형식으로 작성:
            - 🔑 **[핵심 개념]**: 간결하고 명확한 설명 (예: **스택(Stack)**: LIFO 구조의 자료구조로, push/pop 연산을 통해 데이터를 관리)
            - 📌 **[중요도]**: 상(★★★)/중(★★)/하(★) - 시험 출제 빈도 기준
            - 💭 **[암기 팁]**: 연상법이나 기억하기 쉬운 방법 (예: "Last In First Out = LIFO = 늦게 온 사람이 먼저 나간다")
            - 🎯 **[출제 유형]**: 어떤 형식으로 시험에 나올지 예측 (예: 개념 설명, 코드 작성, 응용 문제)
            
            중요: 
            - 각 포인트는 독립적으로 이해 가능해야 함
            - 시험에 직접 출제될 수 있는 형태로 작성
            - 15-20개의 핵심 포인트 추출
            - 우선순위에 따라 정렬 (가장 중요한 것부터)
            
            내용:
            %s
            """.formatted(content);
        
        String response = callAI(prompt, "key-points");
        return parseListResponse(response);
    }
    
    public List<Map<String, Object>> generateQuizzes(String content, int count, String difficulty) {
        log.info("=== Starting quiz generation ===");
        log.info("Count: {}, Difficulty: {}, Content length: {} chars", count, difficulty, content.length());
        
        // 컨텐츠 길이 제한 없음 - 전체 내용 사용
        String contentForQuiz = content;
        
        // 경고만 표시
        if (content.length() > 10000) {
            log.warn("Large content detected: {} chars. This may take longer to process.", content.length());
        }
        
        String prompt = """
            당신은 대학 교수로서 고품질의 평가 문제를 만드는 전문가입니다.
            다음 학습 내용을 바탕으로 %d개의 객관식 문제를 만들어주세요.
            
            난이도: %s
            - EASY (기초): 단순 암기, 정의 확인, 기본 개념 이해 (대학 1-2학년 수준)
            - MEDIUM (중급): 개념 적용, 문제 해결, 분석 능력 (대학 2-3학년 수준)
            - HARD (심화): 종합적 사고, 창의적 응용, 비판적 분석 (대학 3-4학년 수준)
            
            문제 구성 가이드라인:
            1. **개념 이해 문제 (30%%)**: 핵심 개념의 정확한 이해도 평가
               - 정의를 묻는 문제
               - 개념의 특징을 묻는 문제
               - 용어의 의미를 묻는 문제
            
            2. **적용 문제 (30%%)**: 학습한 내용을 새로운 상황에 적용
               - 예시를 들어 개념을 적용하는 문제
               - 실제 상황에서의 활용 문제
               - 계산이나 분석이 필요한 문제
            
            3. **분석/비교 문제 (25%%)**: 여러 개념 간의 관계 이해
               - A와 B의 차이점/공통점
               - 장단점 비교
               - 관계 분석
            
            4. **종합/평가 문제 (15%%)**: 고차원적 사고력 평가
               - 여러 개념을 종합한 문제
               - 비판적 사고가 필요한 문제
               - 새로운 상황에 대한 예측/평가
            
            각 문제는 다음 JSON 형식으로 작성:
            [
                {
                    "question": "명확하고 구체적인 질문 (맥락 포함, 애매하지 않게)",
                    "options": ["선택지1 (정답)", "선택지2 (그럴듯한 오답)", "선택지3 (흔한 오개념)", "선택지4 (관련 있지만 틀린 답)"],
                    "correctAnswer": "정답 선택지 전체 텍스트",
                    "explanation": "왜 이것이 정답인지 상세 설명. 각 오답이 왜 틀렸는지도 설명. 추가 학습 포인트 제시",
                    "difficulty": "%s",
                    "category": "개념이해/적용/분석/종합",
                    "hint": "문제를 풀 때 도움이 되는 힌트 (너무 직접적이지 않게)",
                    "learningObjective": "이 문제로 평가하려는 학습 목표",
                    "commonMistakes": "학생들이 자주 하는 실수"
                }
            ]
            
            문제 작성 시 주의사항:
            1. 선택지는 모두 비슷한 길이와 구조로 작성
            2. 부정문("~아닌 것은?")은 최소화
            3. "모두 맞다", "모두 틀리다" 같은 선택지 지양
            4. 함정이나 말장난이 아닌 실제 이해도를 평가
            5. 선택지 순서는 논리적으로 배치 (숫자는 오름차순, 시간은 순서대로 등)
            6. 각 문제는 독립적으로 풀 수 있어야 함
            7. 실제 대학 시험에 출제될 만한 수준과 형식 유지
            
            내용:
            %s
            """.formatted(count, difficulty, difficulty, contentForQuiz);
        
        log.info("Calling AI to generate quiz questions...");
        String response = callAI(prompt, "quiz-generation");
        log.info("AI response received, parsing quiz data...");
        List<Map<String, Object>> quizzes = parseQuizResponse(response);
        log.info("Successfully generated {} quiz questions", quizzes.size());
        return quizzes;
    }
    
    public String generateStudyPlan(List<Map<String, Object>> courses, List<Map<String, Object>> exams) {
        String prompt = """
            당신은 20년 경력의 학습 컨설턴트입니다.
            학생의 수업 일정과 시험 일정을 분석하여 과학적이고 실현 가능한 맞춤형 학습 계획을 수립해주세요.
            
            수업 일정: %s
            시험 일정: %s
            
            다음 형식으로 구체적이고 실행 가능한 학습 계획을 작성해주세요:
            
            ## 📅 맞춤형 학습 계획
            
            ### 🎯 학습 목표 설정
            #### 단기 목표 (1-2주)
            - 구체적이고 측정 가능한 목표 3개
            - 각 목표별 달성 지표
            
            #### 중기 목표 (1개월)
            - 한 달 후 도달해야 할 학습 수준
            - 평가 방법
            
            #### 장기 목표 (학기 전체)
            - 학기말 목표 성적
            - 전체적인 학습 성과 목표
            
            ### ⏰ 최적화된 일일 학습 스케줄
            #### 평일 스케줄
            - **06:00-08:00**: [추천 활동] - 이 시간대가 좋은 이유
            - **08:00-12:00**: [수업 시간 고려한 학습 계획]
            - **14:00-16:00**: [골든 타임 활용법] - 집중력이 높은 시간
            - **16:00-18:00**: [복습 및 과제]
            - **20:00-22:00**: [심화 학습] - 어려운 과목 집중
            - **22:00-23:00**: [가벼운 복습 및 내일 준비]
            
            #### 주말 스케줄
            - 토요일: 주간 복습 및 부족한 부분 보충
            - 일요일: 다음 주 예습 및 휴식
            
            ### 📊 과목별 학습 시간 배분
            #### 우선순위 설정 (시험 일정 기반)
            1. **[과목명]**: 주당 X시간 
               - 추천 이유: 시험이 Y일 남음, 난이도가 높음
               - 학습 방법: 개념 이해 중심, 문제 풀이 병행
            
            2. **[과목명]**: 주당 X시간
               - 추천 이유: 기초 과목으로 다른 과목의 기반
               - 학습 방법: 꾸준한 복습, 응용력 기르기
            
            ### 🔄 과학적 복습 전략
            #### 에빙하우스 망각곡선 기반 복습 주기
            - **24시간 이내**: 수업 내용 1차 복습 (10분)
            - **3일 후**: 핵심 내용 정리 (20분)
            - **1주일 후**: 전체 내용 복습 + 문제 풀이 (30분)
            - **2주 후**: 심화 문제 및 응용 (40분)
            - **1개월 후**: 종합 복습 및 약점 보완 (1시간)
            
            #### 과목별 복습 방법
            - **암기 과목**: 플래시카드, 반복 학습, 연상법
            - **이해 과목**: 개념도 그리기, 설명하기, 문제 만들기
            - **실습 과목**: 코드 직접 작성, 프로젝트 진행
            
            ### 📝 시험 대비 전략
            #### D-30 (한 달 전)
            - 전체 범위 파악 및 학습 계획 수립
            - 기출문제 수집 및 출제 경향 분석
            - 취약 단원 파악
            
            #### D-14 (2주 전)
            - 집중 학습 기간 시작
            - 단원별 핵심 정리 노트 작성
            - 예상 문제 만들기
            
            #### D-7 (1주 전)
            - 실전 모의고사 풀이
            - 오답 노트 정리
            - 취약 부분 집중 보완
            
            #### D-3 (3일 전)
            - 전체 내용 빠른 복습
            - 핵심 공식/개념 최종 정리
            - 시험 전략 수립 (시간 배분 등)
            
            #### D-1 (전날)
            - 가벼운 복습만
            - 충분한 휴식
            - 시험 준비물 확인
            
            ### 💡 학습 효율 극대화 팁
            #### 집중력 향상 방법
            1. **포모도로 기법**: 25분 집중 + 5분 휴식
            2. **환경 조성**: 조용한 곳, 적절한 조명, 정리된 책상
            3. **디지털 디톡스**: 학습 중 스마트폰 차단
            
            #### 기억력 향상 방법
            1. **능동적 회상**: 책을 덮고 내용 떠올리기
            2. **분산 학습**: 한 번에 몰아서 하지 않기
            3. **다감각 학습**: 보고, 듣고, 쓰고, 말하기
            
            #### 동기부여 유지
            1. **작은 목표 달성**: 매일 성취감 느끼기
            2. **보상 시스템**: 목표 달성 시 자신에게 선물
            3. **학습 일지**: 발전 과정 기록하기
            
            ### ⚠️ 주의사항 및 건강 관리
            #### 번아웃 예방
            - 주 1-2회는 완전한 휴식
            - 취미 활동 시간 확보
            - 규칙적인 운동 (주 3회, 30분)
            
            #### 건강 관리
            - 수면: 최소 7시간 확보
            - 식사: 규칙적인 식사, 뇌에 좋은 음식
            - 스트레칭: 1시간마다 5분 스트레칭
            
            #### 멘탈 관리
            - 명상이나 심호흡
            - 긍정적인 자기 대화
            - 필요시 상담 센터 이용
            
            ### 📈 진도 체크 및 조정
            - 매주 일요일: 주간 학습 성과 평가
            - 2주마다: 학습 계획 조정
            - 매월: 전체적인 진도 점검 및 목표 재설정
            """.formatted(courses.toString(), exams.toString());
        
        return callAI(prompt, "study-plan");
    }
    
    private String callAI(String prompt, String purpose) {
        try {
            log.info("Calling AI for purpose: {}", purpose);
            log.debug("AI Model: {}, OpenAI Key available: {}", preferredModel, !openAiApiKey.isEmpty());
            
            if ("openai".equalsIgnoreCase(preferredModel) && !openAiApiKey.isEmpty()) {
                log.info("Using OpenAI API for {}", purpose);
                return callOpenAI(prompt);
            } else if ("claude".equalsIgnoreCase(preferredModel) && !claudeApiKey.isEmpty()) {
                log.info("Using Claude API for {}", purpose);
                return callClaude(prompt);
            } else {
                log.warn("No AI API key configured. Using mock response for: {}", purpose);
                return getMockResponse(purpose);
            }
        } catch (Exception e) {
            log.error("Error calling AI API for {}: {}", purpose, e.getMessage(), e);
            log.error("Falling back to mock response");
            return getMockResponse(purpose);
        }
    }
    
    private String callOpenAI(String prompt) {
        log.info("Starting OpenAI API call");
        log.debug("OpenAI API URL: {}", openAiUrl);
        log.debug("API Key length: {}", openAiApiKey.length());
        log.debug("Prompt length: {} characters", prompt.length());
        
        WebClient webClient = webClientBuilder
                .baseUrl(openAiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(
                        Map.of("role", "system", "content", 
                                "You are an expert AI tutor specializing in university-level education. " +
                                "You provide detailed, accurate, and helpful responses in Korean. " +
                                "Your responses are well-structured, educational, and tailored for university students."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7,
                "max_tokens", 4000,
                "top_p", 0.9,
                "frequency_penalty", 0.2,
                "presence_penalty", 0.1
        );
        
        try {
            log.info("Sending request to OpenAI API...");
            log.debug("Request body: {}", objectMapper.writeValueAsString(requestBody));
            
            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .block();
            
            log.info("OpenAI API response received successfully");
            log.debug("Response length: {} characters", response != null ? response.length() : 0);
            
            String extractedContent = extractOpenAIResponse(response);
            log.debug("Extracted content length: {} characters", extractedContent.length());
            
            return extractedContent;
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage());
            log.error("Full error details: ", e);
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                log.error("Authentication error - API key may be invalid or expired");
            } else if (e.getMessage() != null && e.getMessage().contains("429")) {
                log.error("Rate limit exceeded");
            } else if (e.getMessage() != null && e.getMessage().contains("500")) {
                log.error("OpenAI server error");
            }
            throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage(), e);
        }
    }
    
    private String callClaude(String prompt) {
        WebClient webClient = webClientBuilder
                .baseUrl(claudeUrl)
                .defaultHeader("x-api-key", claudeApiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        
        Map<String, Object> requestBody = Map.of(
                "model", "claude-3-haiku-20240307",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 1000
        );
        
        return webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractClaudeResponse)
                .block();
    }
    
    private String extractOpenAIResponse(String response) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        } catch (Exception e) {
            log.error("Error extracting OpenAI response: ", e);
        }
        return "";
    }
    
    private String extractClaudeResponse(String response) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
            if (content != null && !content.isEmpty()) {
                return (String) content.get(0).get("text");
            }
        } catch (Exception e) {
            log.error("Error extracting Claude response: ", e);
        }
        return "";
    }
    
    private List<String> parseListResponse(String response) {
        List<String> result = new ArrayList<>();
        String[] lines = response.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            // Remove bullet points, numbers, etc.
            line = line.replaceAll("^[-•*\\d.]+\\s*", "");
            if (!line.isEmpty()) {
                result.add(line);
            }
        }
        return result;
    }
    
    private List<Map<String, Object>> parseQuizResponse(String response) {
        try {
            // JSON 배열 부분만 추출
            String jsonContent = response;
            int startIndex = response.indexOf("[");
            int endIndex = response.lastIndexOf("]");
            
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                jsonContent = response.substring(startIndex, endIndex + 1);
            }
            
            // JSON 파싱
            List<Map<String, Object>> quizzes = objectMapper.readValue(jsonContent, List.class);
            
            // 각 퀴즈의 데이터 검증 및 정리
            for (Map<String, Object> quiz : quizzes) {
                // 필수 필드 확인
                if (!quiz.containsKey("question") || !quiz.containsKey("options") || 
                    !quiz.containsKey("correctAnswer") || !quiz.containsKey("explanation")) {
                    log.warn("Quiz missing required fields: {}", quiz);
                    continue;
                }
                
                // difficulty가 없으면 기본값 설정
                if (!quiz.containsKey("difficulty")) {
                    quiz.put("difficulty", "MEDIUM");
                }
                
                // category가 없으면 기본값 설정
                if (!quiz.containsKey("category")) {
                    quiz.put("category", "개념이해");
                }
                
                // hint가 없으면 기본값 설정
                if (!quiz.containsKey("hint")) {
                    quiz.put("hint", "문제를 차근차근 읽어보고 핵심 개념을 파악해보세요.");
                }
            }
            
            log.debug("Successfully parsed {} quizzes from AI response", quizzes.size());
            return quizzes;
            
        } catch (Exception e) {
            log.error("Failed to parse quiz response as JSON, response: {}", response, e);
            return parsePlainTextQuiz(response);
        }
    }
    
    private List<Map<String, Object>> parsePlainTextQuiz(String response) {
        List<Map<String, Object>> quizzes = new ArrayList<>();
        // 간단한 텍스트 파싱 로직 - 실제 사용시 더 정교한 파싱 필요
        
        Map<String, Object> mockQuiz = new HashMap<>();
        mockQuiz.put("question", "예제 문제입니다.");
        mockQuiz.put("options", List.of("선택지 1", "선택지 2", "선택지 3", "선택지 4"));
        mockQuiz.put("correctAnswer", "선택지 1");
        mockQuiz.put("explanation", "이것이 정답인 이유입니다.");
        mockQuiz.put("difficulty", "MEDIUM");
        
        quizzes.add(mockQuiz);
        return quizzes;
    }
    
    private String getMockResponse(String purpose) {
        switch (purpose) {
            case "summary":
                return """
                    ## 요약
                    
                    이 내용은 주요 개념과 중요한 세부사항을 다루고 있습니다.
                    
                    ### 핵심 개념
                    1. 첫 번째 핵심 개념에 대한 설명
                    2. 두 번째 핵심 개념에 대한 설명
                    
                    ### 주요 내용
                    - 중요한 포인트 1
                    - 중요한 포인트 2
                    
                    ### 결론
                    전체적인 내용을 종합하면 다음과 같습니다.
                    """;
            
            case "key-points":
                return """
                    - 핵심 포인트 1: 중요한 개념에 대한 설명
                    - 핵심 포인트 2: 핵심 정보 강조
                    - 핵심 포인트 3: 기억해야 할 필수 세부사항
                    - 핵심 포인트 4: 중요한 발견 또는 결론
                    """;
            
            case "quiz-generation":
                // 향상된 Mock 퀴즈 데이터
                List<Map<String, Object>> mockQuizzes = new ArrayList<>();
                
                String[] questionTypes = {"CONCEPT", "APPLICATION", "ANALYSIS", "COMPARISON", "TRUE_FALSE"};
                String[] categories = {"핵심개념", "세부사항", "응용문제", "종합이해"};
                
                for (int i = 0; i < 5; i++) {
                    Map<String, Object> quiz = new HashMap<>();
                    String questionType = questionTypes[i % questionTypes.length];
                    String category = categories[i % categories.length];
                    
                    switch (questionType) {
                        case "CONCEPT":
                            quiz.put("question", "Spring Framework의 IoC(Inversion of Control)가 제공하는 주요 이점은 무엇입니까?");
                            quiz.put("options", List.of(
                                "객체 간 결합도를 낮추고 유연성을 높여 테스트와 유지보수가 용이해진다",
                                "실행 속도가 빨라지고 메모리 사용량이 줄어든다",
                                "데이터베이스 연결이 자동으로 관리된다",
                                "컴파일 시간이 단축된다"
                            ));
                            quiz.put("correctAnswer", "객체 간 결합도를 낮추고 유연성을 높여 테스트와 유지보수가 용이해진다");
                            quiz.put("explanation", "IoC는 객체의 생성과 의존관계 설정을 프레임워크가 담당하여 느슨한 결합을 실현합니다.");
                            break;
                            
                        case "APPLICATION":
                            quiz.put("question", "@Transactional 어노테이션을 사용할 때 주의해야 할 점은?");
                            quiz.put("options", List.of(
                                "public 메소드에만 적용되며, 같은 클래스 내부 호출시에는 작동하지 않는다",
                                "private 메소드에만 사용해야 한다",
                                "static 메소드에서만 작동한다",
                                "모든 메소드에 자동으로 적용된다"
                            ));
                            quiz.put("correctAnswer", "public 메소드에만 적용되며, 같은 클래스 내부 호출시에는 작동하지 않는다");
                            quiz.put("explanation", "Spring AOP는 프록시 기반으로 작동하므로 이러한 제약사항이 있습니다.");
                            break;
                            
                        case "ANALYSIS":
                            quiz.put("question", "데이터베이스 인덱스를 과도하게 생성했을 때 발생할 수 있는 문제는?");
                            quiz.put("options", List.of(
                                "INSERT, UPDATE, DELETE 성능이 저하되고 저장 공간이 증가한다",
                                "SELECT 쿼리가 느려진다",
                                "데이터베이스 연결이 불가능해진다",
                                "트랜잭션이 자동으로 롤백된다"
                            ));
                            quiz.put("correctAnswer", "INSERT, UPDATE, DELETE 성능이 저하되고 저장 공간이 증가한다");
                            quiz.put("explanation", "인덱스는 조회 성능을 향상시키지만, 데이터 변경 시 인덱스도 함께 업데이트해야 하므로 쓰기 성능이 저하됩니다.");
                            break;
                            
                        case "COMPARISON":
                            quiz.put("question", "ArrayList와 LinkedList의 차이점으로 올바른 것은?");
                            quiz.put("options", List.of(
                                "ArrayList는 인덱스 접근이 O(1), LinkedList는 삽입/삭제가 O(1)이다",
                                "둘 다 같은 성능을 보인다",
                                "LinkedList가 모든 면에서 더 빠르다",
                                "ArrayList는 삽입/삭제가 더 빠르다"
                            ));
                            quiz.put("correctAnswer", "ArrayList는 인덱스 접근이 O(1), LinkedList는 삽입/삭제가 O(1)이다");
                            quiz.put("explanation", "자료구조 선택은 사용 패턴에 따라 결정해야 합니다. 빈번한 접근이 필요하면 ArrayList, 잦은 삽입/삭제가 필요하면 LinkedList가 유리합니다.");
                            break;
                            
                        case "TRUE_FALSE":
                            quiz.put("question", "Spring Boot는 반드시 내장 톰캣 서버를 사용해야 한다. 이 설명이 맞습니까?");
                            quiz.put("options", List.of(
                                "거짓 - Spring Boot는 Jetty, Undertow 등 다른 서버도 사용 가능하다",
                                "참 - Spring Boot는 톰캣만 지원한다",
                                "거짓 - Spring Boot는 서버를 사용하지 않는다",
                                "참 - 다른 서버를 사용하려면 Spring Framework를 써야 한다"
                            ));
                            quiz.put("correctAnswer", "거짓 - Spring Boot는 Jetty, Undertow 등 다른 서버도 사용 가능하다");
                            quiz.put("explanation", "Spring Boot는 기본적으로 톰캣을 사용하지만, 의존성을 변경하여 다른 서버를 사용할 수 있습니다.");
                            break;
                    }
                    
                    quiz.put("category", category);
                    quiz.put("hint", "핵심 개념을 정확히 이해하고 실제 적용 사례를 생각해보세요.");
                    quiz.put("difficulty", i < 2 ? "EASY" : i < 4 ? "MEDIUM" : "HARD");
                    
                    mockQuizzes.add(quiz);
                }
                
                try {
                    return objectMapper.writeValueAsString(mockQuizzes);
                } catch (Exception e) {
                    return "[]";
                }

            
            case "study-plan":
                return """
                    ## 맞춤형 학습 계획
                    
                    ### 일일 학습 권장 시간
                    - 평일: 3-4시간
                    - 주말: 5-6시간
                    
                    ### 과목별 우선순위
                    1. 시험이 가까운 과목 우선
                    2. 난이도가 높은 과목에 더 많은 시간 할당
                    3. 기초 과목은 꾸준히 복습
                    
                    ### 복습 주기
                    - 새로운 내용: 24시간 이내 첫 복습
                    - 주간 복습: 매주 토요일
                    - 월간 복습: 매월 마지막 주
                    
                    ### 시험 대비 전략
                    - 시험 2주 전부터 집중 복습 시작
                    - 과거 문제 풀이
                    - 요약 노트 작성
                    """;
            
            default:
                return "Mock response for " + purpose;
        }
    }
}
