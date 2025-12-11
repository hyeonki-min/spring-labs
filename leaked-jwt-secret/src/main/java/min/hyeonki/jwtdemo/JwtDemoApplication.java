package min.hyeonki.jwtdemo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import min.hyeonki.jwtdemo.entity.User;
import min.hyeonki.jwtdemo.repository.UserRepository;

@SpringBootApplication
public class JwtDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(JwtDemoApplication.class, args);
	}

	@Bean
    CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                User user1 = User.builder()
                        .username("user1")
                        .password(passwordEncoder.encode("password1"))
                        .email("user1@example.com")
                        .role("USER")
                        .build();

                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin1234"))
                        .email("admin@example.com")
                        .role("ADMIN")
                        .build();

                userRepository.save(user1);
                userRepository.save(admin);
            }
        };
    }
}
