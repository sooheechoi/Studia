package Study.Assistant.Studia.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException e) {
        return createErrorResponse(HttpStatus.CONFLICT, e.getMessage());
    }
    
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException e) {
        return createErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException e) {
        return createErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("errors", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unexpected error occurred: {}", e.getMessage(), e);
        
        // 개발 환경에서는 상세한 오류 메시지 제공
        String message = "An unexpected error occurred";
        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            // 민감한 정보가 포함될 수 있으므로 특정 패턴만 노출
            if (e.getMessage().contains("duplicate") || 
                e.getMessage().contains("constraint") ||
                e.getMessage().contains("validation") ||
                e.getMessage().contains("not found")) {
                message = e.getMessage();
            }
        }
        
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.error("Runtime error occurred: {}", e.getMessage(), e);
        
        // 특정 런타임 예외 처리
        if (e.getMessage() != null) {
            if (e.getMessage().contains("not found")) {
                return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("unauthorized") || e.getMessage().contains("Unauthorized")) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
            } else if (e.getMessage().contains("Invalid") || e.getMessage().contains("validation")) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        }
        
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage() != null ? e.getMessage() : "Internal server error");
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }
    
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Map<String, Object>> handleNumberFormat(NumberFormatException e) {
        log.error("Number format error: {}", e.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Invalid number format");
    }
    
    private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        
        return ResponseEntity.status(status).body(response);
    }
}
