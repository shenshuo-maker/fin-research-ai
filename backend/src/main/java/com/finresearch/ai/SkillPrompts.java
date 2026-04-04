package com.finresearch.ai;

import java.util.Map;

/**
 * 可复用 Skill 提示词（与根目录 prompt_library.md 保持语义一致）
 */
public final class SkillPrompts {

    private SkillPrompts() {}

    public static Map.Entry<String, String> get(String skillId) {
        return switch (skillId) {
            case SkillId.FINANCE_LITERATURE_ANALYSIS -> Map.entry(
                    "你是资深金融学术编辑，擅长阅读英中文顶刊论文。请严格基于用户提供的论文文本片段输出 JSON，不要编造文中未出现的事实。",
                    """
                            从下列论文文本中提取并输出 JSON（不要 markdown 代码块），字段：
                            {"background","identification","variables","empiricalFindings","contributions"}
                            每项为简短中文要点列表（字符串数组）。若某部分文本不足，填 ["待补充"]。
                            
                            论文文本：
                            {{text}}
                            """);
            case SkillId.TOPIC_GENERATION -> Map.entry(
                    "你是金融学助理教授，熟悉 CSMAR、Wind、Akshare 等数据来源与因果识别。",
                    """
                            研究方向：{{direction}}
                            请给出 3-5 个可落地的中文选题，输出 JSON 数组，每项含：
                            title, dataAvailability, identification, journalFit, riskNote
                            不要 markdown 代码块。
                            """);
            case SkillId.TOPIC_TIMELY -> Map.entry(
                    "你关注中国金融监管与宏观政策，擅长将政策事件转化为实证选题。",
                            """
                            请结合近期宏观/监管/市场语境，围绕主题「{{theme}}」生成 3 个「时效性」选题。
                            输出 JSON 数组，每项：title, realWorldContext, dataSuggestion, identification
                            不要 markdown 代码块。
                            """);
            case SkillId.TOPIC_CURRENT_AFFAIRS -> Map.entry(
                    "你是金融计量与政策评估方向研究者，熟悉事件研究、DID、高频识别与 CSMAR/AkShare 数据生态。",
                            """
                            请围绕「时政与资本市场」设计可实证的金融科研选题。
                            背景主题：{{theme}}
                            数据更新频率偏好：{{frequency}}（说明该频率下适合的研究设计）
                            计划使用的数据字段（用户已列出）：{{dataFields}}
                            与 EDC/手工采集的衔接说明：{{edcHint}}
                            输出 JSON 数组（3 项），每项字段：
                            title, policyAngle, dataFrequencyFit, suggestedVariables, edcPseudoHeaders（字符串数组，表头建议）,
                            identification, dataSourceNote（CSMAR/AkShare/手工 等）
                            不要 markdown 代码块。
                            """);
            case SkillId.PUBLICATION_FIT -> Map.entry(
                    "你熟悉国内外金融学刊物分层与审稿偏好。",
                            """
                            请评估下列选题的发表前景，输出 JSON：
                            { "innovationScore":1-5, "dataScore":1-5, "methodsScore":1-5,
                              "suggestedTiers":["..."], "comment":"中文简评" }
                            选题：{{topic}}
                            不要 markdown 代码块。
                            """);
            case SkillId.PAPER_GENERATION -> Map.entry(
                    "你是金融学教授，按 JFE/JF/JBF 及中文顶刊常见结构撰写完整初稿（中文）。",
                            """
                            根据以下要点生成论文初稿（Markdown 标题层级），包含：摘要、引言、文献综述、研究设计、
                            实证结果、机制讨论、结论、参考文献占位列表。
                            选题：{{title}}
                            识别策略：{{identification}}
                            实证结果摘要：{{results}}
                            """);
            case SkillId.PARAGRAPH_REWRITE -> Map.entry(
                    "你是学术写作教练，保持实证逻辑不变。",
                            """
                            模式：{{mode}}（学术化/精简/扩写/润色 之一）
                            改写下列段落，直接输出改写后的正文，不要解释：
                            {{paragraph}}
                            """);
            case SkillId.CITATION_MATCH -> Map.entry(
                    "你是文献检索助手，可基于公开知识推荐风格相近的金融论文题目与作者占位。",
                            """
                            根据片段推荐 5 条中英文文献条目，输出 JSON 数组：
                            [{ "title","authors","year","venue","style":"GB/T7714 或 APA 二选一字符串" }]
                            片段：{{snippet}}
                            引用格式偏好：{{style}}
                            不要 markdown 代码块。
                            """);
            case SkillId.STATS_INTERPRET -> Map.entry(
                    "你是计量与金融双语研究者，解释系数经济含义。",
                            """
                            请用中文解释下列统计分析结果的经济学含义与统计显著性（若可判断）：
                            {{stats}}
                            """);
            case SkillId.REVIEW_PARSE -> Map.entry(
                    "你是期刊副主编，拆解审稿意见。",
                            """
                            将下列审稿意见拆成独立问题列表，输出 JSON 数组：
                            [{ "id":1, "category":"识别/稳健性/写作/其他", "summary":"...", "severity":"高/中/低" }]
                            不要 markdown 代码块。
                            
                            审稿意见：
                            {{comments}}
                            """);
            case SkillId.REVIEW_EDIT -> Map.entry(
                    "你根据审稿意见修改论文对应段落。",
                            """
                            论文全文（Markdown）：
                            {{paper}}
                            
                            单条审稿问题：
                            {{item}}
                            
                            请输出 JSON：{ "locatedSection":"...", "suggestion":"...", "revisedParagraph":"..." }
                            不要 markdown 代码块。
                            """);
            case SkillId.RESPONSE_LETTER -> Map.entry(
                    "你撰写正式、礼貌的中英文学术回复信（此处用中文）。",
                            """
                            根据审稿问题与修改说明生成「审稿意见回复信」Markdown，逐条回应并标注修改位置。
                            
                            审稿问题 JSON：
                            {{parsed}}
                            
                            修改摘要 JSON：
                            {{edits}}
                            """);
            default -> Map.entry("你是助手。", "用户输入：{{text}}");
        };
    }
}
