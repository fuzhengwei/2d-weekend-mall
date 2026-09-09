package cn.xiaofuge.mall.api;

import cn.xiaofuge.mall.domain.Customer;
import cn.xiaofuge.mall.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@RequestBody RegisterRequest request) {
        Customer customer = authService.register(request.username(), request.password(), request.displayName());
        AuthService.LoginResult result = authService.login(request.username(), request.password());
        return Map.of("code", 0, "message", "注册成功", "data", result);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        return Map.of("code", 0, "message", "登录成功", "data", authService.login(request.username(), request.password()));
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        Customer customer = authService.currentCustomer(request);
        if (customer == null) {
            throw new BusinessException("登录已过期，请重新登录", HttpStatus.UNAUTHORIZED);
        }
        return Map.of("code", 0, "message", "ok", "data", customer);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        authService.logout(request);
        return Map.of("code", 0, "message", "已退出登录");
    }

    public record RegisterRequest(@NotBlank String username, @NotBlank String password, String displayName) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }
}
