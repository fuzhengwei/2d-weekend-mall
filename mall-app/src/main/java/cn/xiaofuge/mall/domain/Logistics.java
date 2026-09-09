package cn.xiaofuge.mall.domain;

import java.time.LocalDateTime;
import java.util.List;

public record Logistics(
        String orderNo,
        String trackingNo,
        String carrier,
        String status,
        String currentLocation,
        LocalDateTime estimatedDelivery,
        String pickupCode,
        List<LogisticsEvent> events
) {
    public record LogisticsEvent(LocalDateTime time, String location, String description) {
    }
}
