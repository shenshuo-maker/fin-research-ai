package com.finresearch.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finresearch.config.AnthropicProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final AnthropicProperties anthropicProperties;
    private final ObjectMapper objectMapper;

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .build();
    }

    /**
     * 调用 Claude，variables 用于替换 Skill 模板中的 {{key}}
     */
    public String invokeSkill(String skillId, Map<String, String> variables) {
        var prompt = SkillPrompts.get(skillId);
        String system = prompt.getKey();
        String userTpl = prompt.getValue();
        for (var e : variables.entrySet()) {
            userTpl = userTpl.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
        }
        String key = anthropicProperties.getApiKey();
        if (key == null || key.isBlank()) {
            log.warn("ANTHROPIC_API_KEY 未配置，返回演示占位内容 skill={}", skillId);
            return mockResponse(skillId, variables);
        }
        int attempts = 0;
        Exception last = null;
        while (attempts < 3) {
            attempts++;
            try {
                String body = buildRequestBody(system, userTpl);
                String resp = client().post()
                        .uri("/v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-api-key", key)
                        .header("anthropic-version", "2023-06-01")
                        .body(body)
                        .retrieve()
                        .body(String.class);
                return extractText(resp);
            } catch (Exception e) {
                last = e;
                log.warn("Claude 调用失败 attempt={} {}", attempts, e.getMessage());
                try {
                    Thread.sleep(500L * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        log.error("Claude 多次失败，使用演示占位", last);
        return mockResponse(skillId, variables);
    }

    private String buildRequestBody(String system, String user) throws Exception {
        var root = objectMapper.createObjectNode();
        root.put("model", anthropicProperties.getModel());
        root.put("max_tokens", anthropicProperties.getMaxTokens());
        root.put("system", system);
        var msg = objectMapper.createArrayNode();
        var u = objectMapper.createObjectNode();
        u.put("role", "user");
        u.put("content", user);
        msg.add(u);
        root.set("messages", msg);
        return objectMapper.writeValueAsString(root);
    }

    private String extractText(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode content = root.path("content");
        if (content.isArray() && content.size() > 0) {
            return content.get(0).path("text").asText("");
        }
        return "";
    }

    private String mockResponse(String skillId, Map<String, String> variables) {
        return switch (skillId) {
            case SkillId.FINANCE_LITERATURE_ANALYSIS ->
                    "{\"background\":[\"（演示）研究背景待模型填充\"],\"identification\":[\"DID/RD\"],\"variables\":[\"Y/X/控制变量\"],\"empiricalFindings\":[\"主回归显著\"],\"contributions\":[\"边际贡献示例\"]}";
            case SkillId.TOPIC_GENERATION, SkillId.TOPIC_TIMELY ->
                    "[{\"title\":\"（演示）数字金融与企业创新：基于地级市面板\",\"dataAvailability\":\"CSMAR+地级市指数\",\"identification\":\"双向固定效应\",\"journalFit\":\"中文核心期刊\",\"riskNote\":\"内生性需工具变量\"}]";
            case SkillId.TOPIC_CURRENT_AFFAIRS ->
                    "[{\"title\":\"（演示）注册制改革与新股定价效率：事件研究\",\"policyAngle\":\"资本市场制度变迁\",\"dataFrequencyFit\":\"日频行情+月频财报\",\"suggestedVariables\":[\"收益率\",\"Amihud\",\"承销商声誉\"],\"edcPseudoHeaders\":[\"证券代码\",\"事件日\",\"窗口期超额收益\",\"流动性指标\"],\"identification\":\"事件研究/DID\",\"dataSourceNote\":\"CSMAR 日行情；AkShare 可选补充\"}]";
            case SkillId.PUBLICATION_FIT ->
                    "{\"innovationScore\":3,\"dataScore\":4,\"methodsScore\":3,\"suggestedTiers\":[\"CSSCI\",\"SSCI Q2\"],\"comment\":\"（演示）配置 ANTHROPIC_API_KEY 可生成完整评估。\"}";
            case SkillId.PAPER_GENERATION ->
                    "# （演示）论文初稿\n\n## 摘要\n请配置 API Key 后生成完整稿件。\n\n## 引言\n选题：" + variables.getOrDefault("title", "") + "\n";
            case SkillId.PARAGRAPH_REWRITE ->
                    "（演示改写）" + variables.getOrDefault("paragraph", "");
            case SkillId.CITATION_MATCH ->
                    "[{\"title\":\"Digital Finance and Corporate Innovation\",\"authors\":\"Doe, J.\",\"year\":\"2023\",\"venue\":\"Journal of Financial Economics\",\"style\":\"APA 示例\"}]";
            case SkillId.STATS_INTERPRET -> {
                String s = variables.getOrDefault("stats", "");
                yield "（演示）请在配置 API 后获取完整解读。输入摘要：" + s.substring(0, Math.min(80, s.length()));
            }
            case SkillId.REVIEW_PARSE ->
                    "[{\"id\":1,\"category\":\"稳健性\",\"summary\":\"（演示）请补充异质性分析\",\"severity\":\"中\"}]";
            case SkillId.REVIEW_EDIT ->
                    "{\"locatedSection\":\"结果部分\",\"suggestion\":\"补充稳健性检验\",\"revisedParagraph\":\"（演示）修订段落\"}";
            case SkillId.RESPONSE_LETTER ->
                    "# 审稿意见回复信（演示）\n\n尊敬的编辑与审稿人：\n\n感谢您的意见。我们将在修订稿中补充稳健性检验。\n\n此致\n敬礼";
            default -> "{\"demo\":true}";
        };
    }
}
