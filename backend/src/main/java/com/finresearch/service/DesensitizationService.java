package com.finresearch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finresearch.domain.DesensitizationLog;
import com.finresearch.repository.DesensitizationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DesensitizationService {

    public static final String RULE_VERSION = "v1";

    private final DesensitizationLogRepository logRepository;
    private final ObjectMapper objectMapper;

    private static final List<PatternRule> RULES = List.of(
            new PatternRule("MOBILE", Pattern.compile("1[3-9]\\d{9}")),
            new PatternRule("EMAIL", Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")),
            new PatternRule("ID_CARD", Pattern.compile("\\d{17}[\\dXx]")),
            new PatternRule("BANK_CARD", Pattern.compile("\\d{16,19}"))
    );

    public record DesensitizeResult(String desensitized, List<String> hitTypes, String contentHash) {}

    public DesensitizeResult desensitize(String raw, String sourceType, Long userId) {
        if (raw == null) {
            raw = "";
        }
        List<String> hits = new ArrayList<>();
        String out = raw;
        for (PatternRule r : RULES) {
            if (r.pattern.matcher(out).find()) {
                hits.add(r.name);
                out = r.pattern.matcher(out).replaceAll("[已脱敏:" + r.name + "]");
            }
        }
        // 股票代码样式 6 位数字（保守脱敏，避免误伤日期）
        Pattern stock = Pattern.compile("(?<![0-9])([036]\\d{5})(?![0-9])");
        if (stock.matcher(out).find()) {
            hits.add("STOCK_CODE_LIKE");
            out = stock.matcher(out).replaceAll("[证券代码已泛化]");
        }
        String hash = sha256Short(raw);
        DesensitizationLog log = new DesensitizationLog();
        log.setUserId(userId);
        log.setSourceType(sourceType);
        log.setRuleVersion(RULE_VERSION);
        try {
            log.setHitsJson(objectMapper.writeValueAsString(hits));
        } catch (Exception e) {
            log.setHitsJson("[]");
        }
        log.setContentHash(hash);
        logRepository.save(log);
        return new DesensitizeResult(out, hits, hash);
    }

    private static String sha256Short(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(java.util.Arrays.copyOf(d, 8));
        } catch (Exception e) {
            return "unknown";
        }
    }

    private record PatternRule(String name, Pattern pattern) {}
}
