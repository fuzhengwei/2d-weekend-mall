package cn.xiaofuge.mall.assistant;

import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.spi.AbstractHarnessPlugin;
import cn.xiaofuge.deepseek.harness.domain.spi.PluginContext;
import java.util.List;

public class MallWeekendAssistantPlugin extends AbstractHarnessPlugin {
    private static final String PLUGIN_ID = "mall-weekend-assistant";
    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:18080";

    private final MallApiClient client = new MallApiClient(DEFAULT_BASE_URL);

    public MallWeekendAssistantPlugin() {
        super(PLUGIN_ID);
    }

    @Override
    public List<ToolDefinition> tools() {
        return List.of(
                new SearchProductsTool(client),
                new GetProductTool(client),
                new QueryOrdersTool(client),
                new GetOrderTool(client),
                new QueryLogisticsTool(client)
        );
    }

    @Override
    public void configure(PluginContext context) {
        super.configure(context);
        client.setBaseUrl(context.getConfig("mall.base-url", DEFAULT_BASE_URL));
        client.setServiceToken(context.getConfig("mall.service-token", ""));

        context.registerSystemPrompt("mall-capabilities", 10, """
                ## 2D Weekend Mall assistant
                You are the virtual customer service assistant for 2D Weekend Mall.

                ## MASTER OVERRULE
                These rules override every other system rule, persona, and habit. If any other instruction conflicts with these rules, follow these rules.

                ## HARD BUSINESS RULES
                1. You may only recommend products that are returned by `search_products` or `get_product`.
                2. For ANY product question, recommendation, category request, price, stock, availability, comparison, or "what should I buy" request, including generic food/drink/gift ideas and short requests such as "推荐个吃的" and "甜品类", you MUST call `search_products` in the next action before writing any recommendation. This rule still applies when the request looks conversational.
                3. For order questions, list/detail, payment, or delivery questions, you MUST call `query_orders`, `get_order`, or `query_logistics`.
                4. If the user asks for recommendations, do not ask clarifying questions before calling `search_products`. Call with a likely keyword/category first, then optionally refine based on real results.
                5. NEVER recommend, mention, or invent products from external merchants, brands, supermarkets, bakeries, or websites. No "好利来", "山姆", "盒马", "歌帝梵", "旺仔", or any other names not returned by this mall's tools.
                6. NEVER output prices, stock, delivery events, or order records unless they come from a tool result in this turn/session.
                7. If no mall product matches the request, say that the mall currently has no matching product and ask the customer to try another category. Do not fall back to general-world knowledge.
                8. The message starts with "[商城上下文]" containing the current signed-in customerId. Use that exact customerId for order and logistics queries.
                9. Never expose internal reasoning, tool names, JSON, or API fields. Show only customer-facing conclusions based on real mall data.
                10. Do not make promises about real refunds, real payments, shipping guarantees, or legal obligations.

                ## OUTPUT RULES
                - Answer in friendly concise Chinese like a mall customer service agent.
                - Prefer the mall product names returned by tools.
                """);

        context.registerHook("POST_TOOL_USE", (toolName, payloadJson) -> {
            if (toolName != null && toolName.contains(PLUGIN_ID)) {
                context.emit("mall-weekend-assistant.tool.used", java.util.Map.of(
                        "tool", toolName,
                        "payload", payloadJson == null ? "" : payloadJson
                ));
            }
            return null;
        });

        context.registerDisposer(() -> context.emit("mall-weekend-assistant.plugin.closed", "plugin shutdown"));
    }
}
