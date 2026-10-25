package com.smartclass.smartclass.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartclass.smartclass.annotation.AuthCheck;
import com.smartclass.smartclass.common.BaseResponse;
import com.smartclass.smartclass.common.DeleteRequest;
import com.smartclass.smartclass.common.ErrorCode;
import com.smartclass.smartclass.common.ResultUtils;
import com.smartclass.smartclass.constant.UserConstant;
import com.smartclass.smartclass.exception.BusinessException;
import com.smartclass.smartclass.exception.ThrowUtils;
import com.smartclass.smartclass.model.dto.dailyword.DailyWordAddRequest;
import com.smartclass.smartclass.model.dto.dailyword.DailyWordQueryRequest;
import com.smartclass.smartclass.model.dto.dailyword.DailyWordUpdateRequest;
import com.smartclass.smartclass.model.entity.DailyWord;
import com.smartclass.smartclass.model.entity.User;
import com.smartclass.smartclass.model.vo.DailyWordImportFailItem;
import com.smartclass.smartclass.model.vo.DailyWordImportVO;
import com.smartclass.smartclass.model.vo.DailyWordVO;
import com.smartclass.smartclass.service.DailyWordService;
import com.smartclass.smartclass.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

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
 * 每日单词接口
 */
@RestController
@RequestMapping("/daily-words")
@Slf4j
public class DailyWordController {

    @Resource
    private DailyWordService dailyWordService;

    @Resource
    private UserService userService;


    // region 增删改查

    /**
     * 创建每日单词（仅管理员）
     *
     * @param dailyWordAddRequest 单词创建请求体，包含单词、解释、例句等信息
     * @param request HTTP请求，用于获取当前登录用户
     * @return 新增单词的ID
     */
    @PostMapping("")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addDailyWord(@RequestBody DailyWordAddRequest dailyWordAddRequest,
                                         HttpServletRequest request) {
        if (dailyWordAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DailyWord dailyWord = new DailyWord();
        BeanUtils.copyProperties(dailyWordAddRequest, dailyWord);
        User loginUser = userService.getLoginUser(request);
        Long adminId = loginUser.getId();
        // 设置管理员ID
        dailyWord.setAdminId(adminId);
        // 使用saveDailyWord方法，同步到ES
        boolean result = dailyWordService.saveDailyWord(dailyWord);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(dailyWord.getId());
    }

    /**
     * 删除每日单词（仅管理员）
     *
     * @param id 要删除单词的ID
     * @return 是否删除成功
     */
    @DeleteMapping("/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteDailyWord(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        DailyWord oldDailyWord = dailyWordService.getById(id);
        ThrowUtils.throwIf(oldDailyWord == null, ErrorCode.NOT_FOUND_ERROR);
        // 使用deleteDailyWord方法，同步删除ES中的数据
        boolean b = dailyWordService.deleteDailyWord(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新每日单词（仅管理员）
     *
     * @param id 单词ID
     * @param dailyWordUpdateRequest 单词更新请求，包含需要更新的单词信息
     * @return 是否更新成功
     */
    @PutMapping("/{id}/admin")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateDailyWord(@PathVariable("id") Long id, 
                                              @RequestBody DailyWordUpdateRequest dailyWordUpdateRequest) {
        if (dailyWordUpdateRequest == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 设置ID
        dailyWordUpdateRequest.setId(id);
        DailyWord dailyWord = new DailyWord();
        BeanUtils.copyProperties(dailyWordUpdateRequest, dailyWord);
        // 判断是否存在
        DailyWord oldDailyWord = dailyWordService.getById(id);
        ThrowUtils.throwIf(oldDailyWord == null, ErrorCode.NOT_FOUND_ERROR);
        // 使用updateDailyWord方法，同步更新ES中的数据
        boolean result = dailyWordService.updateDailyWord(dailyWord);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取每日单词
     *
     * @param id 单词ID
     * @return 单词视图对象
     */
    @GetMapping("/{id}")
    public BaseResponse<DailyWordVO> getDailyWordVOById(@PathVariable("id") Long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DailyWord dailyWord = dailyWordService.getById(id);
        if (dailyWord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 返回单词详情
        return ResultUtils.success(dailyWordService.getDailyWordVO(dailyWord));
    }

    /**
     * 分页获取单词列表（仅管理员）
     *
     * @param dailyWordQueryRequest 单词查询请求，包含分页参数和查询条件
     * @return 单词分页列表
     */
    @GetMapping("/admin/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<DailyWord>> listDailyWordByPage(DailyWordQueryRequest dailyWordQueryRequest) {
        if (dailyWordQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = dailyWordQueryRequest.getCurrent();
        long size = dailyWordQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        QueryWrapper<DailyWord> queryWrapper = dailyWordService.getQueryWrapper(dailyWordQueryRequest);
        Page<DailyWord> dailyWordPage = dailyWordService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(dailyWordPage);
    }

    /**
     * 分页获取单词列表（封装VO）
     *
     * @param dailyWordQueryRequest 单词查询请求，包含分页参数和查询条件
     * @return 单词视图对象分页列表
     */
    @GetMapping("/page")
    public BaseResponse<Page<DailyWordVO>> listDailyWordVOByPage(DailyWordQueryRequest dailyWordQueryRequest) {
        if (dailyWordQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = dailyWordQueryRequest.getCurrent();
        long size = dailyWordQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<DailyWord> dailyWordPage = dailyWordService.page(new Page<>(current, size),
                dailyWordService.getQueryWrapper(dailyWordQueryRequest));
        Page<DailyWordVO> dailyWordVOPage = new Page<>(current, size, dailyWordPage.getTotal());
        List<DailyWordVO> dailyWordVOList = dailyWordService.getDailyWordVO(dailyWordPage.getRecords());
        dailyWordVOPage.setRecords(dailyWordVOList);
        return ResultUtils.success(dailyWordVOPage);
    }

    /**
     * 获取今日单词
     *
     * @param difficulty 难度等级，可选参数，用于筛选特定难度的单词
     * @return 随机单词
     */
    @GetMapping("/today")
    public BaseResponse<DailyWordVO> getTodayWord(
            @RequestParam(required = false) Integer difficulty) {
        DailyWordVO dailyWordVO = dailyWordService.getRandomDailyWord(difficulty);
        if (dailyWordVO == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到单词");
        }
        return ResultUtils.success(dailyWordVO);
    }

    /**
     * 从ES搜索单词
     *
     * @param searchText 搜索关键词，服务层会自动匹配单词的所有相关字段
     * @return 符合搜索条件的单词视图对象分页列表
     */
    @GetMapping("/search")
    public BaseResponse<Page<DailyWordVO>> searchDailyWord(@RequestParam String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        }
        // 调用服务层方法进行搜索，service层负责匹配所有字段
        Page<DailyWord> dailyWordPage = dailyWordService.searchFromEs(searchText);
        Page<DailyWordVO> dailyWordVOPage = new Page<>(dailyWordPage.getCurrent(), 
                dailyWordPage.getSize(), dailyWordPage.getTotal());
        List<DailyWordVO> dailyWordVOList = dailyWordService.getDailyWordVO(dailyWordPage.getRecords());
        dailyWordVOPage.setRecords(dailyWordVOList);
        return ResultUtils.success(dailyWordVOPage);
    }

    /**
     * CSV 批量导入每日单词（仅管理员）
     *
     * <p>CSV 列：word、translation 必填；pronunciation、example、exampleTranslation、
     * category、difficulty(1-4)、audioUrl、notes、publishDate(YYYY-MM-DD) 选填。
     * 与管理端 Import 页面下载的模板一致。</p>
     *
     * @param file    CSV 文件（multipart 字段名 file）
     * @param request HTTP 请求，用于获取当前登录用户
     * @return 导入结果统计（总数/成功/失败/失败明细）
     */
    @PostMapping("/import")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<DailyWordImportVO> importDailyWords(@RequestParam("file") MultipartFile file,
                                                            HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 CSV 文件");
        }
        User loginUser = userService.getLoginUser(request);

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

            dailyWord.setAdminId(loginUser.getId());
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
        return ResultUtils.success(result);
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