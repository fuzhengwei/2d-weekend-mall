package cn.xiaofuge.mall.assistant;

import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

class QueryOrdersTool extends AbstractMallTool {
    QueryOrdersTool(MallApiClient client) {
        super(client);
    }

    @Override
    public String name() {
        return "query_orders";
    }

    @Override
    public String description() {
        return "查询指定演示用户的虚拟订单列表，包含状态和金额。";
    }

    @Override
    public Map<String, Object> parameters() {
        return objectSchema()
                .prop("customerId", stringSchema("商城上下文中的当前登录用户 ID"))
                .build();
    }

    @Override
    protected CompletableFuture<ToolExecutionResult> run(Map<String, Object> args, ToolRunContext ctx) {
        String customerId = str(args, "customerId", "").trim();
        if (customerId.isBlank()) {
            return fail("customerId 不能为空", "INVALID_ARGUMENT");
        }
        return callMall("/api/mall/orders?customerId=" + customerId);
    }
}
