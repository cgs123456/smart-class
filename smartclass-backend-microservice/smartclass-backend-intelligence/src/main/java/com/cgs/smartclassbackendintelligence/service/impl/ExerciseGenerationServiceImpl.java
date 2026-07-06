package com.cgs.smartclassbackendintelligence.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cgs.smartclassbackendcommon.common.ErrorCode;
import com.cgs.smartclassbackendcommon.exception.BusinessException;
import com.cgs.smartclassbackendintelligence.config.DifyConfig;
import com.cgs.smartclassbackendintelligence.service.AiAvatarService;
import com.cgs.smartclassbackendintelligence.service.ExerciseGenerationService;
import com.cgs.smartclassbackendintelligence.service.WrongQuestionService;
import com.cgs.smartclassbackendintelligence.utils.OkHttpUtils;
import com.cgs.smartclassbackendmodel.model.entity.AiAvatar;
import com.cgs.smartclassbackendmodel.model.entity.Course;
import com.cgs.smartclassbackendmodel.model.entity.CourseChapter;
import com.cgs.smartclassbackendmodel.model.entity.CourseSection;
import com.cgs.smartclassbackendmodel.model.entity.WrongQuestion;
import com.cgs.smartclassbackendserviceclient.service.CourseFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 习题生成服务实现
 * 通过 Feign 获取课程内容，调用 Dify 生成习题，落库到 wrong_question 表
 */
@Service
@Slf4j
public class ExerciseGenerationServiceImpl implements ExerciseGenerationService {

    @Resource
    private CourseFeignClient courseFeignClient;

    @Resource
    private WrongQuestionService wrongQuestionService;

    @Resource
    private AiAvatarService aiAvatarService;

    @Resource
    private DifyConfig difyConfig;

    @Resource
    private OkHttpUtils okHttpUtils;

    @Value("${exercise.enabled:false}")
    private boolean exerciseEnabled;

    @Value("${exercise.ai-avatar-id:}")
    private String aiAvatarIdStr;

    @Value("${exercise.count-per-generate:5}")
    private int countPerGenerate;

    @Override
    public int generateExercises(Long courseId, Long chapterId, int questionCount, Long operatorId) {
        if (!exerciseEnabled) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "AI习题生成功能未启用");
        }
        if (courseId == null || courseId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程ID不能为空");
        }
        if (StrUtil.isBlank(aiAvatarIdStr)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未配置习题生成AI分身");
        }
        Long aiAvatarId;
        try {
            aiAvatarId = Long.parseLong(aiAvatarIdStr.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "习题生成AI分身ID配置无效");
        }
        if (questionCount <= 0) {
            questionCount = countPerGenerate;
        }

        // 1. 获取 AI 分身
        AiAvatar aiAvatar = aiAvatarService.getById(aiAvatarId);
        if (aiAvatar == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "AI分身不存在");
        }
        if (StrUtil.isBlank(aiAvatar.getBaseUrl()) || StrUtil.isBlank(aiAvatar.getAvatarAuth())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI分身配置不完整");
        }

        // 2. 获取课程信息（Feign）
        Course course;
        try {
            course = courseFeignClient.getCourseById(courseId);
        } catch (Exception e) {
            log.error("Feign调用获取课程失败: courseId={}", courseId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取课程信息失败");
        }
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }

        // 3. 获取章节列表（Feign）
        List<CourseChapter> chapters;
        try {
            chapters = courseFeignClient.listChapters(courseId);
        } catch (Exception e) {
            log.error("Feign调用获取章节失败: courseId={}", courseId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取章节信息失败");
        }
        if (chapters == null) {
            chapters = new ArrayList<>();
        }
        // 过滤指定章节
        CourseChapter targetChapter = null;
        if (chapterId != null) {
            List<CourseChapter> filtered = chapters.stream()
                    .filter(c -> chapterId.equals(c.getId()))
                    .collect(Collectors.toList());
            if (filtered.isEmpty()) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "指定章节不存在");
            }
            targetChapter = filtered.get(0);
        }

        // 4. 获取小节列表（Feign）
        List<CourseSection> sections;
        try {
            sections = courseFeignClient.listSections(courseId);
        } catch (Exception e) {
            log.error("Feign调用获取小节失败: courseId={}", courseId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取小节信息失败");
        }
        if (sections == null) {
            sections = new ArrayList<>();
        }
        // 若指定章节，则过滤出该章节下的小节
        if (chapterId != null) {
            final Long chId = chapterId;
            sections = sections.stream()
                    .filter(s -> chId.equals(s.getChapterId()))
                    .collect(Collectors.toList());
        }

        // 5. 构建 prompt
        String prompt = buildPrompt(course, targetChapter, sections, questionCount);

        // 6. 调用 Dify（直接通过 OkHttpUtils，不经过 DifyServiceImpl，避免聊天历史持久化）
        String answer = callDify(aiAvatar, prompt, operatorId);

        // 7. 解析 JSON 数组
        List<WrongQuestion> exercises = parseExercises(answer, courseId, chapterId, operatorId);
        if (exercises.isEmpty()) {
            log.warn("AI未生成有效习题, courseId={}, answer={}", courseId, answer);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI未返回有效习题");
        }

        // 8. 批量保存
        boolean saved = wrongQuestionService.saveBatch(exercises);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "习题保存失败");
        }
        log.info("AI习题生成成功: courseId={}, chapterId={}, 生成 {} 道习题", courseId, chapterId, exercises.size());
        return exercises.size();
    }

    @Override
    public List<WrongQuestion> getCourseExercises(Long courseId, Long chapterId) {
        if (courseId == null || courseId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程ID不能为空");
        }
        QueryWrapper<WrongQuestion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("courseId", courseId)
                .eq("aiGenerated", 1)
                .eq("isDelete", 0)
                .eq(chapterId != null, "chapterId", chapterId)
                .orderByDesc("createTime");
        return wrongQuestionService.list(queryWrapper);
    }

    /**
     * 构建习题生成 prompt
     */
    private String buildPrompt(Course course, CourseChapter chapter, List<CourseSection> sections, int questionCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个课程习题生成助手。请根据以下课程内容生成 ").append(questionCount).append(" 道练习题。\n\n");
        sb.append("课程信息：\n");
        sb.append("- 课程名称：").append(StrUtil.nullToEmpty(course.getTitle())).append("\n");
        sb.append("- 课程描述：").append(StrUtil.nullToEmpty(course.getDescription())).append("\n");
        if (chapter != null) {
            sb.append("- 章节名称：").append(StrUtil.nullToEmpty(chapter.getTitle())).append("\n");
            sb.append("- 章节描述：").append(StrUtil.nullToEmpty(chapter.getDescription())).append("\n");
        }
        if (sections != null && !sections.isEmpty()) {
            sb.append("- 包含小节：\n");
            for (CourseSection section : sections) {
                sb.append("  - ").append(StrUtil.nullToEmpty(section.getTitle()))
                        .append("（").append(StrUtil.nullToEmpty(section.getDescription())).append("）\n");
            }
        }
        sb.append("\n要求：\n");
        sb.append("1. 生成选择题和简答题的混合\n");
        sb.append("2. 选择题需包含4个选项（A/B/C/D）\n");
        sb.append("3. 每题需包含正确答案和解析\n");
        sb.append("4. 难度分为简单(1)、中等(2)、困难(3)\n\n");
        sb.append("请以 JSON 数组格式返回：\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"questionType\": \"choice\",\n");
        sb.append("    \"questionContent\": \"题目内容\",\n");
        sb.append("    \"options\": [\"A. 选项A\", \"B. 选项B\", \"C. 选项C\", \"D. 选项D\"],\n");
        sb.append("    \"correctAnswer\": \"A\",\n");
        sb.append("    \"analysis\": \"解析说明\",\n");
        sb.append("    \"difficulty\": 2\n");
        sb.append("  },\n");
        sb.append("  {\n");
        sb.append("    \"questionType\": \"short_answer\",\n");
        sb.append("    \"questionContent\": \"简答题内容\",\n");
        sb.append("    \"options\": [],\n");
        sb.append("    \"correctAnswer\": \"参考答案\",\n");
        sb.append("    \"analysis\": \"解析说明\",\n");
        sb.append("    \"difficulty\": 1\n");
        sb.append("  }\n");
        sb.append("]\n\n");
        sb.append("只返回 JSON 数组，不要添加其他文字。");
        return sb.toString();
    }

    /**
     * 直接调用 Dify /chat-messages（blocking 模式），返回 answer 文本
     */
    private String callDify(AiAvatar aiAvatar, String prompt, Long operatorId) {
        String chatPath = StrUtil.isNotBlank(difyConfig.getChatMessagesPath())
                ? difyConfig.getChatMessagesPath() : "/chat-messages";
        String url = aiAvatar.getBaseUrl() + chatPath;

        JSONObject body = new JSONObject();
        body.set("inputs", new JSONObject());
        body.set("query", prompt);
        body.set("response_mode", "blocking");
        body.set("user", difyConfig.getUserPrefix() + operatorId);
        body.set("auto_generate_name", false);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + aiAvatar.getAvatarAuth());

        String requestJson = body.toString();
        log.info("调用Dify生成习题: url={}, operatorId={}", url, operatorId);

        Response response = okHttpUtils.postJson(url, requestJson, headers);
        try {
            if (!response.isSuccessful()) {
                String errBody = "";
                try (ResponseBody rb = response.body()) {
                    if (rb != null) {
                        errBody = rb.string();
                    }
                } catch (IOException e) {
                    log.error("读取错误响应体异常", e);
                }
                log.error("Dify API错误: code={}, body={}", response.code(), errBody);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用AI服务失败: " + response.code());
            }
            String respBody = "";
            try (ResponseBody rb = response.body()) {
                if (rb != null) {
                    respBody = rb.string();
                }
            } catch (IOException e) {
                log.error("读取响应体异常", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取AI响应失败");
            }
            JSONObject respJson = JSONUtil.parseObj(respBody);
            String answer = respJson.getStr("answer");
            if (StrUtil.isBlank(answer)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI返回内容为空");
            }
            return answer;
        } finally {
            response.close();
        }
    }

    /**
     * 从 answer 中解析习题列表
     * 兼容 markdown ```json 包裹及额外说明文字
     */
    private List<WrongQuestion> parseExercises(String answer, Long courseId, Long chapterId, Long operatorId) {
        List<WrongQuestion> list = new ArrayList<>();
        if (StrUtil.isBlank(answer)) {
            return list;
        }
        // 提取首个 '[' 到末尾 ']' 的内容
        int start = answer.indexOf('[');
        int end = answer.lastIndexOf(']');
        if (start < 0 || end <= start) {
            log.error("AI返回内容未找到JSON数组: {}", answer);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI返回格式不正确");
        }
        String jsonArrayStr = answer.substring(start, end + 1);
        JSONArray jsonArray;
        try {
            jsonArray = JSONUtil.parseArray(jsonArrayStr);
        } catch (Exception e) {
            log.error("解析习题JSON数组失败: {}", jsonArrayStr, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI返回内容解析失败");
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            try {
                JSONObject obj = jsonArray.getJSONObject(i);
                WrongQuestion q = new WrongQuestion();
                q.setUserId(operatorId);
                q.setQuestionType(obj.getStr("questionType"));
                q.setQuestionContent(obj.getStr("questionContent"));
                // options 数组转 JSON 字符串存储
                JSONArray optionsArr = obj.getJSONArray("options");
                if (optionsArr != null) {
                    q.setOptions(optionsArr.toString());
                } else {
                    q.setOptions("[]");
                }
                q.setCorrectAnswer(obj.getStr("correctAnswer"));
                q.setAnalysis(obj.getStr("analysis"));
                q.setSourceType("course");
                q.setCourseId(courseId);
                q.setChapterId(chapterId);
                Integer difficulty = obj.getInt("difficulty");
                q.setDifficulty(difficulty == null ? 1 : difficulty);
                q.setAiGenerated(1);
                q.setMasteryLevel(0);
                q.setReviewCount(0);

                if (StrUtil.isBlank(q.getQuestionType()) || StrUtil.isBlank(q.getQuestionContent())
                        || StrUtil.isBlank(q.getCorrectAnswer())) {
                    log.warn("跳过不完整的习题: {}", obj);
                    continue;
                }
                list.add(q);
            } catch (Exception e) {
                log.warn("解析第 {} 道习题失败", i, e);
            }
        }
        return list;
    }
}
