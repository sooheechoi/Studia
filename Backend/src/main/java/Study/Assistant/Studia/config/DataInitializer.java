package Study.Assistant.Studia.config;

import Study.Assistant.Studia.domain.entity.User;
import Study.Assistant.Studia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Bean
    @Profile("!test") // Don't run in test environment
    CommandLineRunner init() {
        return args -> {
            log.info("Initializing application data...");
            
            // Create default users if they don't exist
            createDefaultUsers();
            
            log.info("Data initialization completed.");
        };
    }
    
    private void createDefaultUsers() {
        // Create admin user
        if (!userRepository.existsByEmail("admin@studia.com")) {
            User admin = User.builder()
                    .email("admin@studia.com")
                    .password(passwordEncoder.encode("Admin123!"))
                    .name("Admin")
                    .university("Studia University")
                    .role(User.UserRole.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Admin user created: admin@studia.com");
        }
        
        // Create test users
        for (int i = 1; i <= 3; i++) {
            String email = "test" + i + "@example.com";
            if (!userRepository.existsByEmail(email)) {
                User testUser = User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("Test1234!"))
                        .name("TestUser" + i)
                        .university("Test University")
                        .major("Computer Science")
                        .grade(i)
                        .role(User.UserRole.STUDENT)
                        .build();
                userRepository.save(testUser);
                log.info("Test user created: {}", email);
            }
        }
        
        // Create demo user for presentation
        if (!userRepository.existsByEmail("demo@studia.com")) {
            User demo = User.builder()
                    .email("demo@studia.com")
                    .password(passwordEncoder.encode("Demo1234!"))
                    .name("DemoUser")
                    .university("Demo University")
                    .major("Information Technology")
                    .grade(3)
                    .role(User.UserRole.STUDENT)
                    .build();
            userRepository.save(demo);
            log.info("Demo user created: demo@studia.com");
        }
    }
}
