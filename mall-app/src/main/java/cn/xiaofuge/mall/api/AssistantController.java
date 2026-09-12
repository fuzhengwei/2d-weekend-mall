package cn.xiaofuge.mall.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import cn.xiaofuge.mall.domain.Customer;
import cn.xiaofuge.mall.service.MallService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final ObjectMapper objectMapper;

    private final String harnessBaseUrl;
    private final String agentId;
    private final String channelCode;
    private final String approvalMode;
    private final HttpClient httpClient;
    private final MallService mallService;

    public AssistantController(
            ObjectMapper objectMapper,
            @Value("${mall.assistant.harness-base-url:http://127.0.0.1:8090}") String harnessBaseUrl,
            @Value("${mall.assistant.agent-id:customer-service-demo}") String agentId,
            @Value("${mall.assistant.channel-code:}") String channelCode,
            @Value("${mall.assistant.approval-mode:FULL_OPEN}") String approvalMode,
            MallService mallService
    ) {
        this.objectMapper = objectMapper;
        this.harnessBaseUrl = harnessBaseUrl.endsWith("/") ? harnessBaseUrl.substring(0, harnessBaseUrl.length() - 1) : harnessBaseUrl;
        this.agentId = agentId;
        this.channelCode = channelCode;
        this.approvalMode = approvalMode;
        this.mallService = mallService;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void stream(@RequestBody AssistantMessageRequest request,
                       @RequestAttribute("currentCustomer") Customer customer,
                       HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(harnessBaseUrl + "/api/agent/stream"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofMinutes(5))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json(customer, request.message()).getBytes(StandardCharsets.UTF_8)))
                    .build();
            HttpResponse<InputStream> upstream = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (upstream.statusCode() / 100 != 2) {
                response.getWriter().println("event: error");
                response.getWriter().println("data: {\"content\":\"智能客服暂时不可用，请稍后再试。\"}");
                return;
            }
            try (InputStream input = upstream.body(); OutputStream output = response.getOutputStream()) {
                input.transferTo(output);
                output.flush();
            }
        } catch (Exception exception) {
            response.getWriter().println("event: error");
            response.getWriter().println("data: {\"content\":\"无法连接智能客服服务，请确认 Harness 已启动。\"}");
        }
    }

    private String json(Customer customer, String message) throws IOException {
        String userMessage = message == null ? "" : message;
        StringBuilder contextualMessage = new StringBuilder();
        contextualMessage.append("[商城上下文] 当前登录用户ID：")
                .append(customer.id())
                .append("；用户昵称：")
                .append(customer.displayName())
                .append('\n');
        contextualMessage.append("""
                [业务路由] 这是 2D Weekend Mall 客服。商品、推荐、价格、库存问题必须先调用 search_products；
                订单、物流、支付问题必须先调用 query_orders/get_order/query_logistics。
                禁止使用商城工具之外的任何商品、品牌、价格或订单数据。
                """);
        if (mentionsProducts(userMessage)) {
            contextualMessage.append("\n[商品工具路由] 用户请求需要商城商品数据。请立即调用 search_products 工具查询商城商品；不要先追问偏好，也不要使用商城外知识。\n");
        }
        if (mentionsOrders(userMessage)) {
            contextualMessage.append("\n[当前用户订单] 只能使用下面的真实订单。\n");
            contextualMessage.append(objectMapper.writeValueAsString(mallService.listOrders(customer.id())));
        }
        contextualMessage.append('\n').append(userMessage);
        return buildPayload(customer, contextualMessage.toString());
    }

    private boolean mentionsOrders(String message) {
        return message != null && List.of("订单", "物流", "快递", "发货", "配送", "支付", "退款").stream()
                .anyMatch(message::contains);
    }

    private boolean mentionsProducts(String message) {
        return message != null && List.of(
                "商品", "推荐", "吃的", "食品", "零食", "甜品", "喝", "饮料", "价格", "库存",
                "多少钱", "买", "找", "有没有", "搜索", "查商品"
        ).stream().anyMatch(message::contains);
    }

    private String buildPayload(Customer customer, String contextualMessage) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("agentId", agentId);
        if (channelCode != null && !channelCode.isBlank()) {
            payload.put("channelCode", channelCode);
        }
        payload.put("approvalMode", approvalMode);
        payload.put("message", contextualMessage);
        payload.put("sessionId", null);
        return objectMapper.writeValueAsString(payload);
    }

    public record AssistantMessageRequest(String message) {
    }
}
