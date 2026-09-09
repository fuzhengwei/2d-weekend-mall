package cn.xiaofuge.mall.api;

import cn.xiaofuge.mall.domain.CartItem;
import cn.xiaofuge.mall.domain.Customer;
import cn.xiaofuge.mall.domain.Logistics;
import cn.xiaofuge.mall.domain.Order;
import cn.xiaofuge.mall.domain.Product;
import cn.xiaofuge.mall.service.MallService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/mall")
public class MallController {
    private final MallService mallService;

    public MallController(MallService mallService) {
        this.mallService = mallService;
    }

    @GetMapping("/products")
    public Map<String, Object> products(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "全部") String category
    ) {
        List<Product> items = mallService.searchProducts(keyword, category);
        return Map.of("code", 0, "message", "ok", "data", items);
    }

    @GetMapping("/products/{productId}")
    public Map<String, Object> product(@PathVariable String productId) {
        return Map.of("code", 0, "message", "ok", "data", mallService.getProduct(productId));
    }

    @GetMapping("/cart")
    public Map<String, Object> cart(@RequestAttribute("currentCustomer") Customer customer) {
        return cartResponse(mallService.getCart(customer.id()));
    }

    @PostMapping("/cart/items")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addToCart(
            @RequestAttribute("currentCustomer") Customer customer,
            @RequestBody AddCartItemRequest request
    ) {
        return cartResponse(mallService.addToCart(
                customer.id(), request.productId(), request.quantity()));
    }

    @PutMapping("/cart/items/{productId}")
    public Map<String, Object> updateCartItem(
            @RequestAttribute("currentCustomer") Customer customer,
            @PathVariable String productId,
            @RequestBody UpdateCartItemRequest request
    ) {
        return cartResponse(mallService.updateCartItem(
                customer.id(), productId, request.quantity()));
    }

    @DeleteMapping("/cart/items/{productId}")
    public Map<String, Object> removeCartItem(
            @RequestAttribute("currentCustomer") Customer customer,
            @PathVariable String productId
    ) {
        return cartResponse(mallService.removeCartItem(customer.id(), productId));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> placeOrder(
            @RequestAttribute("currentCustomer") Customer customer,
            @RequestBody PlaceOrderRequest request
    ) {
        Order order = mallService.placeOrder(customer.id(),
                request.receiver(), request.phone(), request.address(), request.paymentMethod(), request.remark());
        return Map.of("code", 0, "message", "下单成功", "data", order);
    }

    @GetMapping("/orders")
    public Map<String, Object> orders(@RequestAttribute("currentCustomer") Customer customer) {
        List<Order> items = mallService.listOrders(customer.id());
        return Map.of("code", 0, "message", "ok", "data", items);
    }

    @GetMapping("/orders/{orderNo}")
    public Map<String, Object> order(
            @RequestAttribute("currentCustomer") Customer customer,
            @PathVariable String orderNo
    ) {
        return Map.of("code", 0, "message", "ok",
                "data", mallService.getOrder(customer.id(), orderNo));
    }

    @PostMapping("/orders/{orderNo}/payment")
    public Map<String, Object> payOrder(
            @RequestAttribute("currentCustomer") Customer customer,
            @PathVariable String orderNo,
            @RequestBody(required = false) PaymentRequest request
    ) {
        Order order = mallService.payOrder(customer.id(), orderNo,
                request == null ? null : request.paymentMethod());
        return Map.of("code", 0, "message", "支付成功", "data", order);
    }

    @PostMapping("/orders/{orderNo}/cancel")
    public Map<String, Object> cancelOrder(
            @RequestAttribute("currentCustomer") Customer customer,
            @PathVariable String orderNo
    ) {
        return Map.of("code", 0, "message", "订单已取消",
                "data", mallService.cancelOrder(customer.id(), orderNo));
    }

    @GetMapping("/orders/{orderNo}/logistics")
    public Map<String, Object> logistics(
            @RequestAttribute("currentCustomer") Customer customer,
            @PathVariable String orderNo
    ) {
        Logistics data = mallService.getLogistics(customer.id(), orderNo);
        return Map.of("code", 0, "message", "ok", "data", data);
    }

    private Map<String, Object> cartResponse(Map<String, Object> cart) {
        return Map.of("code", 0, "message", "ok", "data", cart);
    }

    public record AddCartItemRequest(@NotBlank String productId, @Min(1) @Max(10) int quantity) {
    }

    public record UpdateCartItemRequest(@Min(0) @Max(10) int quantity) {
    }

    public record PlaceOrderRequest(
            @NotBlank String receiver,
            @NotBlank String phone,
            @NotBlank String address,
            @NotBlank String paymentMethod,
            String remark
    ) {
    }

    public record PaymentRequest(String paymentMethod) {
    }
}
