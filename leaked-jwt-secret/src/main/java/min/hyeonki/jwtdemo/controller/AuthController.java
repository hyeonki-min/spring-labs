package min.hyeonki.jwtdemo.controller;

import min.hyeonki.jwtdemo.entity.User;
import min.hyeonki.jwtdemo.service.JwtService;
import min.hyeonki.jwtdemo.service.UserService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService,
                          JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        User user = userService.authenticate(request.getUsername(), request.getPassword());
        String token = jwtService.createAccessToken(user);
        return ResponseEntity.ok(new TokenResponse(token));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class TokenResponse {
        private final String accessToken;
    }
}
