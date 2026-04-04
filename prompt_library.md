# 提示词工程库（Claude Skill）

与后端代码 `com.finresearch.ai.SkillId`、`SkillPrompts` **一一对应**，修改时请同步更新 Java 源文件。

## Skill 列表

| Skill ID | 用途 |
|----------|------|
| `skill_finance_literature_analysis` | 金融文献 PDF 文本 → 结构化 JSON |
| `skill_topic_generation_finance` | 研究方向 → 多个可落地选题 |
| `skill_topic_timely_policy` | 时效性政策/市场主题 → 热点选题 |
| `skill_topic_current_affairs_finance` | 时政/政策焦点 + 频率 + 数据字段 + EDC 提示 → 选题 JSON（含伪表头） |
| `skill_publication_fit_assessment` | 单选题 → 创新度/数据/方法评分与期刊建议（前端已下线，接口保留） |
| `skill_paper_generation` | 选题+识别+结果 → 中文论文初稿 Markdown |
| `skill_paragraph_rewrite` | 段落 + 模式（学术化/精简/扩写/润色） |
| `skill_citation_match` | 片段 → 推荐文献 JSON（含 GB/T 7714 或 APA 样式字段） |
| `skill_stats_interpretation_finance` | 统计结果文本 → 经济含义解读 |
| `skill_peer_review_parse` | 审稿意见 → 分类问题 JSON 列表 |
| `skill_peer_review_edit` | 全文 + 单条意见 → 修改建议 JSON |
| `skill_response_letter` | 解析后意见 + 修改摘要 → 回复信 Markdown |

## 模板变量

各 Skill 用户消息模板中使用 `{{variable}}` 占位，运行时由 `AIService.invokeSkill(skillId, Map)` 替换。

| Skill | 变量 |
|-------|------|
| literature | `text` |
| topic generation | `direction` |
| timely | `theme` |
| current affairs | `theme`, `frequency`, `dataFields`, `edcHint` |
| publication fit | `topic` |
| paper | `title`, `identification`, `results` |
| rewrite | `paragraph`, `mode` |
| citation | `snippet`, `style` |
| stats | `stats` |
| review parse | `comments` |
| review edit | `paper`, `item` |
| response letter | `parsed`, `edits` |

## 系统提示（摘要）

- 文献：资深金融学术编辑，输出严格 JSON，不编造。
- 选题：熟悉 CSMAR、Wind、Akshare 与因果识别。
- 论文：按顶刊常见章节结构中文撰写。
- 回修：副主编视角拆解；回复信正式、礼貌、逐条回应。

完整系统提示与用户模板见源码 `SkillPrompts.java`。
