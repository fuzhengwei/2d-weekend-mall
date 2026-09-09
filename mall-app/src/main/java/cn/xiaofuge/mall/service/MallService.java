package cn.xiaofuge.mall.service;

import cn.xiaofuge.mall.api.BusinessException;
import cn.xiaofuge.mall.domain.CartItem;
import cn.xiaofuge.mall.domain.Logistics;
import cn.xiaofuge.mall.domain.Order;
import cn.xiaofuge.mall.domain.OrderItem;
import cn.xiaofuge.mall.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MallService {
    private static final String DEFAULT_CUSTOMER = "customer-1";

    private final Map<String, Product> products = MallData.products().stream()
            .collect(Collectors.toMap(Product::id, item -> item, (left, right) -> left, LinkedHashMap::new));
    private final Map<String, Map<String, Integer>> carts = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final Map<String, Logistics> logistics = new ConcurrentHashMap<>();
    private final AtomicLong orderSequence = new AtomicLong(1024);

    public String normalizeCustomer(String customerId) {
        return customerId == null || customerId.isBlank() ? DEFAULT_CUSTOMER : customerId.trim();
    }

    public List<Product> searchProducts(String keyword, String category) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String normalizedCategory = category == null || category.isBlank() || "全部".equals(category)
                ? "" : category.trim();
        return products.values().stream()
                .filter(item -> matchesKeyword(item, normalizedKeyword))
                .filter(item -> normalizedCategory.isEmpty() || item.category().equals(normalizedCategory))
                .toList();
    }

    public Product getProduct(String productId) {
        Product product = products.get(productId);
        if (product == null) {
            throw new BusinessException("商品不存在或已下架", HttpStatus.NOT_FOUND);
        }
        return product;
    }

    public Map<String, Object> getCart(String customerId) {
        List<CartItem> items = toCartItems(customerId);
        BigDecimal total = items.stream().map(CartItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("customerId", customerId, "items", items, "totalAmount", total);
    }

    public Map<String, Object> addToCart(String customerId, String productId, int quantity) {
        Product product = getProduct(productId);
        if (quantity < 1 || quantity > 10) {
            throw new BusinessException("单次加入数量需在 1-10 之间");
        }
        Map<String, Integer> cart = carts.computeIfAbsent(customerId, key -> new ConcurrentHashMap<>());
        int nextQuantity = cart.merge(productId, quantity, Integer::sum);
        if (nextQuantity > product.stock()) {
            cart.put(productId, product.stock());
            throw new BusinessException("库存不足，最多可购买 " + product.stock() + " 件");
        }
        return getCart(customerId);
    }

    public Map<String, Object> updateCartItem(String customerId, String productId, int quantity) {
        getProduct(productId);
        Map<String, Integer> cart = carts.computeIfAbsent(customerId, key -> new ConcurrentHashMap<>());
        if (quantity <= 0) {
            cart.remove(productId);
        } else if (quantity > 10) {
            throw new BusinessException("单品最多购买 10 件");
        } else {
            cart.put(productId, quantity);
        }
        return getCart(customerId);
    }

    public Map<String, Object> removeCartItem(String customerId, String productId) {
        return updateCartItem(customerId, productId, 0);
    }

    public synchronized Order placeOrder(
            String customerId, String receiver, String phone, String address, String paymentMethod, String remark
    ) {
        List<CartItem> cartItems = toCartItems(customerId);
        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车是空的，先挑一件周末好物吧");
        }
        if (receiver == null || receiver.isBlank() || phone == null || phone.isBlank()
                || address == null || address.isBlank()) {
            throw new BusinessException("收件人、手机号和地址不能为空");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new BusinessException("请选择支付方式");
        }

        List<OrderItem> orderItems = cartItems.stream()
                .map(item -> new OrderItem(item.productId(), item.name(), item.brand(), item.emoji(),
                        item.unitPrice(), item.quantity(), item.subtotal()))
                .toList();
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(customerId);
        order.setItems(orderItems);
        order.setTotalAmount(orderItems.stream().map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        order.setStatus("CREATED");
        order.setPaymentMethod(paymentMethod);
        order.setReceiver(receiver.trim());
        order.setPhone(phone.trim());
        order.setAddress(address.trim());
        order.setCreatedAt(LocalDateTime.now());
        order.setRemark(remark == null ? "" : remark.trim());
        orders.put(order.getOrderNo(), order);
        carts.remove(customerId);
        return order;
    }

    public List<Order> listOrders(String customerId) {
        return orders.values().stream()
                .filter(order -> order.getCustomerId().equals(customerId))
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList();
    }

    public Order getOrder(String customerId, String orderNo) {
        Order order = requireOrder(orderNo);
        assertOwner(order, customerId);
        return order;
    }

    public synchronized Order payOrder(String customerId, String orderNo, String paymentMethod) {
        Order order = getOrder(customerId, orderNo);
        if ("PAID".equals(order.getStatus())) {
            throw new BusinessException("订单已支付，无需重复支付");
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("订单已取消，无法支付");
        }
        String method = paymentMethod == null || paymentMethod.isBlank()
                ? order.getPaymentMethod() : paymentMethod;
        if (!List.of("WECHAT", "ALIPAY", "BANK_CARD").contains(method)) {
            throw new BusinessException("不支持的支付方式");
        }
        order.setPaymentMethod(method);
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        logistics.put(orderNo, createLogistics(orderNo));
        return order;
    }

    public synchronized Order cancelOrder(String customerId, String orderNo) {
        Order order = getOrder(customerId, orderNo);
        if (!"CREATED".equals(order.getStatus())) {
            throw new BusinessException("仅待支付订单可以取消");
        }
        order.setStatus("CANCELLED");
        return order;
    }

    public Logistics getLogistics(String customerId, String orderNo) {
        getOrder(customerId, orderNo);
        Logistics result = logistics.get(orderNo);
        if (result == null) {
            throw new BusinessException("订单支付后才会生成物流信息", HttpStatus.NOT_FOUND);
        }
        return result;
    }

    private boolean matchesKeyword(Product product, String keyword) {
        if (keyword.isEmpty()) {
            return true;
        }
        return product.name().toLowerCase(Locale.ROOT).contains(keyword)
                || product.brand().toLowerCase(Locale.ROOT).contains(keyword)
                || product.category().contains(keyword)
                || product.companyName().toLowerCase(Locale.ROOT).contains(keyword)
                || product.workPolicy().contains(keyword)
                || product.tagline().contains(keyword)
                || product.description().contains(keyword)
                || product.tags().stream().anyMatch(tag -> tag.contains(keyword));
    }

    private List<CartItem> toCartItems(String customerId) {
        Map<String, Integer> cart = carts.getOrDefault(customerId, Map.of());
        return cart.entrySet().stream()
                .map(entry -> {
                    Product product = products.get(entry.getKey());
                    return product == null ? null : new CartItem(product.id(), product.name(), product.brand(),
                            product.emoji(), product.price(), entry.getValue());
                })
                .filter(item -> item != null)
                .sorted(Comparator.comparing(CartItem::productId))
                .toList();
    }

    private Order requireOrder(String orderNo) {
        Order order = orders.get(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在", HttpStatus.NOT_FOUND);
        }
        return order;
    }

    private void assertOwner(Order order, String customerId) {
        if (!order.getCustomerId().equals(customerId)) {
            throw new BusinessException("无权查看该订单", HttpStatus.FORBIDDEN);
        }
    }

    private String generateOrderNo() {
        return "MD" + LocalDateTime.now().format(DateTimeFormats.DATE_COMPACT)
                + String.format("%06d", orderSequence.incrementAndGet());
    }

    private Logistics createLogistics(String orderNo) {
        LocalDateTime now = LocalDateTime.now();
        List<Logistics.LogisticsEvent> events = List.of(
                new Logistics.LogisticsEvent(now, "上海周末仓", "包裹已打包，准备出库"),
                new Logistics.LogisticsEvent(now.plusMinutes(20), "上海周末仓", "虚拟承运商已揽收"),
                new Logistics.LogisticsEvent(now.plusHours(4), "城市转运中心", "包裹到达转运中心"),
                new Logistics.LogisticsEvent(now.plusHours(18), "周末生活驿站", "驿站分拣完成，待派送"),
                new Logistics.LogisticsEvent(now.plusDays(1), "您的楼下", "快递员正在派送")
        );
        return new Logistics(orderNo, "WL" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT),
                "2D 周末快递", "运输中", "城市转运中心", now.plusDays(2), "8888", events);
    }

    private static final class DateTimeFormats {
        private static final java.time.format.DateTimeFormatter DATE_COMPACT =
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
    }
}
