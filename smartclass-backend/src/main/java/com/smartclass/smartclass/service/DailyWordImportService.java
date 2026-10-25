package com.smartclass.smartclass.service;

import com.smartclass.smartclass.common.ErrorCode;
import com.smartclass.smartclass.exception.BusinessException;
import com.smartclass.smartclass.model.entity.DailyWord;
import com.smartclass.smartclass.model.vo.DailyWordImportFailItem;
import com.smartclass.smartclass.model.vo.DailyWordImportVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 每日单词 CSV 批量导入。
 *
 * <p>契约对齐管理端 Import.tsx：表头 word/translation 必填，其余列
 * pronunciation/example/exampleTranslation/category/difficulty(1-4)/audioUrl/notes/publishDate(YYYY-MM-DD)
 * 选填；列顺序无关。逐行校验并收集失败明细，任何单行失败不影响其余行。
 */
@Service
public class DailyWordImportService {

    @Resource
    private DailyWordService dailyWordService;

    public DailyWordImportVO importCsv(MultipartFile file, Long adminUserId) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "读取文件失败");
        }
        if (!lines.isEmpty()) {
            lines.set(0, lines.get(0).replace("﻿", ""));
        }

        // 表头列名 -> 列下标，允许列顺序与模板不同
        List<String> header = parseCsvLine(lines.get(0));
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            colIndex.put(header.get(i).trim().toLowerCase(), i);
        }
        if (!colIndex.containsKey("word") || !colIndex.containsKey("translation")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "CSV 表头缺少必填列：word / translation");
        }

        DailyWordImportVO result = new DailyWordImportVO();
        List<DailyWordImportFailItem> failList = new ArrayList<>();
        int success = 0;
        int total = 0;
        for (int rowNo = 2; rowNo <= lines.size(); rowNo++) {
            String raw = lines.get(rowNo - 1);
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            total++;
            List<String> cols = parseCsvLine(raw);
            DailyWordImportFailItem failItem = new DailyWordImportFailItem();
            failItem.setRow(rowNo);

            String word = csvField(cols, colIndex, "word");
            String translation = csvField(cols, colIndex, "translation");
            if (word == null || word.isEmpty() || translation == null || translation.isEmpty()) {
                failItem.setWord(word);
                failItem.setReason("单词或翻译不能为空");
                failList.add(failItem);
                continue;
            }
            failItem.setWord(word);

            if (dailyWordService.lambdaQuery().eq(DailyWord::getWord, word).count() > 0) {
                failItem.setReason("单词已存在");
                failList.add(failItem);
                continue;
            }

            DailyWord dailyWord = new DailyWord();
            dailyWord.setWord(word);
            dailyWord.setTranslation(translation);
            dailyWord.setPronunciation(csvField(cols, colIndex, "pronunciation"));
            dailyWord.setExample(csvField(cols, colIndex, "example"));
            dailyWord.setExampleTranslation(csvField(cols, colIndex, "exampleTranslation"));
            dailyWord.setCategory(csvField(cols, colIndex, "category"));
            dailyWord.setAudioUrl(csvField(cols, colIndex, "audioUrl"));
            dailyWord.setNotes(csvField(cols, colIndex, "notes"));

            String difficultyStr = csvField(cols, colIndex, "difficulty");
            if (difficultyStr != null && !difficultyStr.isEmpty()) {
                try {
                    int difficulty = Integer.parseInt(difficultyStr.trim());
                    if (difficulty < 1 || difficulty > 4) {
                        throw new NumberFormatException();
                    }
                    dailyWord.setDifficulty(difficulty);
                } catch (NumberFormatException e) {
                    failItem.setReason("难度必须为 1-4 的整数");
                    failList.add(failItem);
                    continue;
                }
            }

            String publishDateStr = csvField(cols, colIndex, "publishDate");
            if (publishDateStr != null && !publishDateStr.isEmpty()) {
                try {
                    LocalDate localDate = LocalDate.parse(publishDateStr.trim());
                    dailyWord.setPublishDate(
                            Date.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
                } catch (DateTimeParseException e) {
                    failItem.setReason("发布日期格式应为 YYYY-MM-DD");
                    failList.add(failItem);
                    continue;
                }
            }

            dailyWord.setAdminId(adminUserId);
            try {
                boolean saved = dailyWordService.saveDailyWord(dailyWord);
                if (saved) {
                    success++;
                } else {
                    failItem.setReason("保存失败");
                    failList.add(failItem);
                }
            } catch (Exception e) {
                failItem.setReason("保存失败：" + e.getMessage());
                failList.add(failItem);
            }
        }

        result.setTotal(total);
        result.setSuccess(success);
        result.setFail(failList.size());
        result.setFailList(failList);
        return result;
    }

    /**
     * 解析单行 CSV，支持双引号包裹的逗号字段与 "" 转义
     */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }

    /**
     * 按表头名取列值，越界或缺失返回 null
     */
    private String csvField(List<String> cols, Map<String, Integer> colIndex, String name) {
        Integer idx = colIndex.get(name.toLowerCase());
        if (idx == null || idx >= cols.size()) {
            return null;
        }
        String value = cols.get(idx);
        return value == null || value.isEmpty() ? null : value;
    }
}
