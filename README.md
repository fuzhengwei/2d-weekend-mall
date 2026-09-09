# 2D Weekend Mall

一个 PC 端虚拟商城：商品搜索、加入购物车、下单、支付、订单查询和物流跟踪都在浏览器内闭环。项目同时包含一个可安装到 `deepseek-harness-java` 的智能客服 Java Native 插件。

## 核心能力

- 商城服务：Spring Boot 提供商品、购物车、订单、支付、物流 REST API。
- 虚拟业务闭环：数据保存在内存中，预置用户 `customer-1` 与 12 件商品。
- 智能客服插件：注册商品搜索、商品详情、订单查询、订单详情和物流查询工具。
- Harness 集成：插件通过系统提示词说明工具边界，并通过 `POST_TOOL_USE` 发出客服审计事件。
- PC 界面：原生 HTML/CSS/JS，无前端构建依赖。

## 启动商城

商城服务本身无需数据库插件，可直接启动：

```bash
cd /Users/fuzhengwei/DevOps/2d-weekend-mall
mvn -pl mall-app spring-boot:run
```

打开 <http://localhost:18080>。默认演示用户是 `customer-1`。

> 重要：页面右上角的 **AI 客服** 不是商城独立能力。必须先在 `deepseek-harness-java` 中导入并启动 `mall-weekend-assistant` 插件，否则客服无法调用商品、订单和物流工具。完整接入流程见 Harness 仓库的 [`README.md`](../deepseek-harness-java/README.md)。

## 安装客服插件

先编译本项目，插件 JAR 在 `mall-agent-plugin/target/mall-agent-plugin-1.0.0-SNAPSHOT.jar`：

```bash
mvn package
```

在 `deepseek-harness-java` 的 `harness.yml` 中预装，或在宿主运行后调用安装接口：

```bash
curl -X POST http://localhost:8090/api/harness/plugins/install \
  -H 'Content-Type: application/json' \
  -d '{
    "pluginId": "mall-weekend-assistant",
    "displayName": "2D Weekend Mall Assistant",
    "pluginVersion": "1.0.0-SNAPSHOT",
    "runtimeType": "JAVA_NATIVE",
    "sourcePath": "/Users/fuzhengwei/DevOps/2d-weekend-mall/mall-agent-plugin/target/mall-agent-plugin-1.0.0-SNAPSHOT.jar",
    "entrypoint": "mall-agent-plugin-1.0.0-SNAPSHOT.jar"
  }'

curl -X POST http://localhost:8090/api/harness/plugins/run \
  -H 'Content-Type: application/json' \
  -d '{"pluginId":"mall-weekend-assistant"}'
```

宿主加载后，Agent 会看到 `plugin__mall-weekend-assistant__search_products` 等工具。

在 Harness 插件配置中设置 `mall.service-token`，值需要与商城的
`mall.security.service-token` 一致。客服插件通过这个服务凭证查询当前登录用户的订单和物流。
同时建议设置 `mall.base-url` 为 `http://127.0.0.1:18080`。保存插件配置后，停用并重新启用插件，确保插件重新执行 `configure(context)`。

## 客服助手

商城页面右上角提供 AI 客服面板。`/api/assistant/stream` 会把用户消息代理到 Harness 的 `/api/agent/stream`，默认 Agent 是 `customer-service-demo`。可在 `mall-app/src/main/resources/application.yml` 调整：

```yaml
mall:
  assistant:
    harness-base-url: http://localhost:8090
    agent-id: customer-service-demo
```

插件工具默认调用 `http://localhost:18080`，可通过 Harness 插件配置 `mall.base-url` 覆盖。

## 项目结构

- `mall-app`：商城后端和 PC 前端。
- `mall-agent-plugin`：DeepSeek Harness 插件。
- `docs/DESIGN.md`：业务、架构和插件能力设计。
