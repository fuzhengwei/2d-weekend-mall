package cn.xiaofuge.mall.api;

import cn.xiaofuge.mall.domain.Customer;
import cn.xiaofuge.mall.service.AuthService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFilter implements Filter {
    private final AuthService authService;
    private final String serviceToken;

    public AuthenticationFilter(
            AuthService authService,
            @Value("${mall.security.service-token:mall-internal-demo-token}") String serviceToken
    ) {
        this.authService = authService;
        this.serviceToken = serviceToken;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String incomingServiceToken = request.getHeader("X-Service-Token");
        boolean serviceAuthorized = incomingServiceToken != null && incomingServiceToken.equals(serviceToken);
        Customer customer = serviceAuthorized ? null : authService.currentCustomer(request);
        boolean publicApi = request.getRequestURI().startsWith("/api/auth/")
                || request.getRequestURI().startsWith("/api/mall/products");
        if (publicApi || customer != null || serviceAuthorized) {
            if (customer != null) {
                request.setAttribute("currentCustomer", customer);
            }
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"请先登录\"}");
    }
}
