package cn.xiaofuge.mall.assistant;

import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

class GetOrderTool extends AbstractMallTool {
    GetOrderTool(MallApiClient client) {
        super(client);
    }

    @Override
    public String name() {
        return "get_order";
    }

    @Override
    public String description() {
        return "查询一笔虚拟商城订单的明细、状态、金额和收货信息。";
    }

    @Override
    public Map<String, Object> parameters() {
        return objectSchema()
                .prop("customerId", stringSchema("商城上下文中的当前登录用户 ID"))
                .prop("orderNo", stringSchema("商城订单号，例如 MD20260909102500"))
                .required("orderNo")
                .build();
    }

    @Override
    protected CompletableFuture<ToolExecutionResult> run(Map<String, Object> args, ToolRunContext ctx) {
        String customerId = str(args, "customerId", "").trim();
        if (customerId.isBlank()) {
            return fail("customerId 不能为空", "INVALID_ARGUMENT");
        }
        String orderNo = str(args, "orderNo", "").trim();
        return callMall("/api/mall/orders/" + orderNo + "?customerId=" + customerId);
    }
}
