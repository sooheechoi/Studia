package Study.Assistant.Studia.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class InputSanitizer {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<script[^>]*>.*?</script>|<iframe[^>]*>.*?</iframe>|javascript:|onerror=|onload=",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    /**
     * HTML 태그를 제거하고 안전한 텍스트로 변환
     */
    public String sanitizeHtml(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "");
    }
    
    /**
     * XSS 공격 패턴 검사
     */
    public boolean containsXSS(String input) {
        if (input == null) return false;
        return XSS_PATTERN.matcher(input).find();
    }
    
    /**
     * 이메일 형식 검증
     */
    public boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 파일명 검증 및 정리
     */
    public String sanitizeFileName(String fileName) {
        if (fileName == null) return null;
        // Remove path separators and special characters
        return fileName.replaceAll("[/\\\\:*?\"<>|]", "_")
                      .replaceAll("\\s+", "_")
                      .replaceAll("_{2,}", "_");
    }
    
    /**
     * SQL Injection 방지를 위한 기본 검증
     */
    public boolean containsSQLKeywords(String input) {
        if (input == null) return false;
        String upperInput = input.toUpperCase();
        String[] sqlKeywords = {"SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "UNION", "WHERE", 
                                "OR 1=1", "'; --", "/*", "*/"};
        for (String keyword : sqlKeywords) {
            if (upperInput.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 안전한 정수 변환
     */
    public Integer parseIntSafely(String input, Integer defaultValue) {
        if (input == null || input.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
