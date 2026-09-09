package cn.xiaofuge.mall.domain;

import java.math.BigDecimal;

public record CartItem(
        String productId,
        String name,
        String brand,
        String emoji,
        BigDecimal unitPrice,
        int quantity
) {
    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
