package cn.xiaofuge.mall.domain;

import java.math.BigDecimal;

public record OrderItem(
        String productId,
        String name,
        String brand,
        String emoji,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {
}
