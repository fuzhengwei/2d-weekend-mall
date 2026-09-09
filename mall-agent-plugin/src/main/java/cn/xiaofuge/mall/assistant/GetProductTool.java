package cn.xiaofuge.mall.assistant;

import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

class GetProductTool extends AbstractMallTool {
    GetProductTool(MallApiClient client) {
        super(client);
    }

    @Override
    public String name() {
        return "get_product";
    }

    @Override
    public String description() {
        return "查询单个虚拟商品的完整详情、价格、库存和周末使用建议。";
    }

    @Override
    public Map<String, Object> parameters() {
        return objectSchema()
                .prop("productId", stringSchema("商品 ID，例如 p001"))
                .required("productId")
                .build();
    }

    @Override
    protected CompletableFuture<ToolExecutionResult> run(Map<String, Object> args, ToolRunContext ctx) {
        return callMall("/api/mall/products/" + str(args, "productId", "").trim());
    }
}
