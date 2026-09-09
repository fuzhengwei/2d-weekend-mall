package cn.xiaofuge.mall.assistant;

import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

class SearchProductsTool extends AbstractMallTool {
    SearchProductsTool(MallApiClient client) {
        super(client);
    }

    @Override
    public String name() {
        return "search_products";
    }

    @Override
    public String description() {
        return "按关键字或分类搜索 2D Weekend Mall 中的虚拟商品，返回价格、库存、评分和简介。";
    }

    @Override
    public Map<String, Object> parameters() {
        return objectSchema()
                .prop("keyword", stringSchema("商品名称、品牌、标签、用途或描述，可为空"))
                .prop("category", stringSchema("分类，如 居家、咖啡、香薰、旅行、数码"))
                .build();
    }

    @Override
    protected CompletableFuture<ToolExecutionResult> run(Map<String, Object> args, ToolRunContext ctx) {
        String keyword = str(args, "keyword", "");
        String category = str(args, "category", "全部");
        String path = "/api/mall/products?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                + "&category=" + URLEncoder.encode(category, StandardCharsets.UTF_8);
        return callMall(path);
    }
}
