package min.hyeonki.jwtdemo.service;


import min.hyeonki.jwtdemo.entity.User;
import min.hyeonki.jwtdemo.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtUtil jwtUtil;

    public JwtService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public String createAccessToken(User user) {
        return jwtUtil.generateToken(user.getId(), user.getRole());
    }

    public Long extractUserId(String token) {
        return jwtUtil.getUserId(token);
    }

    public String extractRole(String token) {
        return jwtUtil.getRole(token);
    }

    public Claims parseClaims(String token) {
        return jwtUtil.parseClaims(token);
    }

    public boolean isExpired(String token) {
        return jwtUtil.isExpired(token);
    }
}
