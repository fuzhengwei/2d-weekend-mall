package cn.xiaofuge.mall.service;

import cn.xiaofuge.mall.api.BusinessException;
import cn.xiaofuge.mall.domain.Customer;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final long SESSION_TTL_SECONDS = 24 * 60 * 60;

    private final Map<String, Customer> customers = new ConcurrentHashMap<>();
    private final Map<String, String> passwordHashes = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService() {
        Customer customer = new Customer("customer-1", "customer-1", "小傅");
        customers.put(customer.id(), customer);
        passwordHashes.put(customer.id(), hash("123456"));
    }

    public Customer register(String username, String password, String displayName) {
        String normalizedUsername = normalize(username);
        if (!normalizedUsername.matches("^[a-zA-Z][a-zA-Z0-9_-]{2,31}$")) {
            throw new BusinessException("用户名需以字母开头，长度 3-32，可包含数字、下划线和短横线");
        }
        if (password == null || password.length() < 6 || password.length() > 64) {
            throw new BusinessException("密码长度需在 6-64 位之间");
        }
        if (customers.values().stream().anyMatch(item -> item.username().equals(normalizedUsername))) {
            throw new BusinessException("用户名已被注册");
        }
        String customerId = "customer-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Customer customer = new Customer(
                customerId,
                normalizedUsername,
                normalizeText(displayName, 1, 32, normalizedUsername)
        );
        customers.put(customer.id(), customer);
        passwordHashes.put(customer.id(), hash(password));
        return customer;
    }

    public LoginResult login(String username, String password) {
        Customer customer = customers.values().stream()
                .filter(item -> item.username().equals(normalize(username)))
                .findFirst()
                .orElseThrow(() -> new BusinessException("用户名或密码错误", HttpStatus.UNAUTHORIZED));
        if (!hash(password).equals(passwordHashes.get(customer.id()))) {
            throw new BusinessException("用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }
        String token = newToken();
        sessions.put(token, new Session(customer.id(), Instant.now().plusSeconds(SESSION_TTL_SECONDS)));
        return new LoginResult(token, customer);
    }

    public Customer currentCustomer(HttpServletRequest request) {
        String token = bearerToken(request);
        if (token == null) {
            return null;
        }
        Session session = sessions.get(token);
        if (session == null || session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return null;
        }
        return customers.get(session.customerId());
    }

    public Customer currentCustomerById(String customerId) {
        return customerId == null || customerId.isBlank() ? null : customers.get(customerId.trim());
    }

    public void logout(HttpServletRequest request) {
        String token = bearerToken(request);
        if (token != null) {
            sessions.remove(token);
        }
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        String headerToken = request.getHeader("X-Auth-Token");
        return headerToken == null || headerToken.isBlank() ? null : headerToken.trim();
    }

    private String hash(String password) {
        try {
            byte[] salt = "2d-weekend-mall-demo-salt".getBytes(StandardCharsets.UTF_8);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return Base64.getEncoder().encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("密码处理失败", exception);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeText(String value, int minLength, int maxLength, String fallback) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return fallback;
        }
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            throw new BusinessException("昵称长度需在 " + minLength + "-" + maxLength + " 位之间");
        }
        return normalized;
    }

    public record LoginResult(String token, Customer customer) {
    }

    private record Session(String customerId, Instant expiresAt) {
    }
}
