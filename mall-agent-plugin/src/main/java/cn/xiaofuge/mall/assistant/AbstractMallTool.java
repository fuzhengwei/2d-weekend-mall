package cn.xiaofuge.mall.assistant;

import cn.xiaofuge.deepseek.harness.domain.model.entity.AbstractTool;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import java.util.concurrent.CompletableFuture;

abstract class AbstractMallTool extends AbstractTool {
    final MallApiClient client;

    AbstractMallTool(MallApiClient client) {
        this.client = client;
    }

    @Override
    public boolean isConcurrencySafe(Object args) {
        return true;
    }

    protected CompletableFuture<ToolExecutionResult> callMall(String path) {
        try {
            return ok(client.get(path));
        } catch (Exception exception) {
            return fail(exception.getMessage(), "MALL_API_ERROR");
        }
    }
}
