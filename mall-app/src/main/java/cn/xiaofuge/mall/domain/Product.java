package cn.xiaofuge.mall.domain;

import java.math.BigDecimal;
import java.util.List;

public record Product(
        String id,
        String name,
        String brand,
        String category,
        String emoji,
        String tagline,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        int stock,
        double rating,
        List<String> tags,
        String weekendTip,
        String companyName,
        String workPolicy
) {
}
