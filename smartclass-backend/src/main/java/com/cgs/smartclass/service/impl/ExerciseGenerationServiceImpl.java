package com.cgs.smartclass.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.config.DifyConfig;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.model.dto.dify.DifyChatRequest;
import com.cgs.smartclass.model.entity.AiAvatar;
import com.cgs.smartclass.model.entity.Course;
import com.cgs.smartclass.model.entity.CourseChapter;
import com.cgs.smartclass.model.entity.CourseSection;
import com.cgs.smartclass.model.entity.WrongQuestion;
import com.cgs.smartclass.service.AiAvatarService;
import com.cgs.smartclass.service.CourseChapterService;
import com.cgs.smartclass.service.CourseSectionService;
import com.cgs.smartclass.service.CourseService;
import com.cgs.smartclass.service.ExerciseGenerationService;
import com.cgs.smartclass.service.WrongQuestionService;
import com.cgs.smartclass.utils.OkHttpUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 习题生成服务实现
 */
@Service
@Slf4j
public class ExerciseGenerationServiceImpl implements ExerciseGenerationService {

    @Resource
    private CourseService courseService;

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private CourseSectionService courseSectionService;

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
    private String exerciseAiAvatarId;

    @Value("${exercise.count-per-generate:5}")
    private int defaultCountPerGenerate;

    @Override
    public int generateExercises(Long courseId, Long chapterId, int questionCount, Long operatorId) {
        // 1. 校验启用状态
        if (!exerciseEnabled) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI习题生成功能未启用");
        }
        if (courseId == null || courseId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程ID不能为空");
        }
        if (operatorId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "操作人ID不能为空");
        }
        if (StrUtil.isBlank(exerciseAiAvatarId)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI习题生成功能未配置AI分身ID");
        }
        if (questionCount <= 0) {
            questionCount = defaultCountPerGenerate;
        }

        // 2. 获取 AI 分身
        AiAvatar aiAvatar = aiAvatarService.getById(Long.parseLong(exerciseAiAvatarId));
        if (aiAvatar == null || StrUtil.isBlank(aiAvatar.getBaseUrl()) || StrUtil.isBlank(aiAvatar.getAvatarAuth())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "习题生成AI分身配置无效");
        }

        // 3. 读取课程内容
        Course course = courseService.getById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "课程不存在");
        }

        CourseChapter chapter = null;
        if (chapterId != null && chapterId > 0) {
            chapter = courseChapterService.getById(chapterId);
            if (chapter == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "章节不存在");
            }
        }

        // 查询小节列表
        QueryWrapper<CourseSection> sectionWrapper = new QueryWrapper<>();
        sectionWrapper.eq("courseId", courseId);
        if (chapterId != null && chapterId > 0) {
            sectionWrapper.eq("chapterId", chapterId);
        }
        sectionWrapper.eq("isDelete", 0);
        sectionWrapper.orderByAsc("sort");
        List<CourseSection> sections = courseSectionService.list(sectionWrapper);

        // 4. 构建课程内容文本（用于 inputs）
        String courseContent = buildCourseContent(course, chapter, sections);

        // 5. 构建 prompt
        String prompt = buildPrompt(course, chapter, sections, questionCount);

        // 6. 调用 Dify
        String answer = callDify(aiAvatar, prompt, courseContent, operatorId);

        // 7. 解析习题
        JSONArray exerciseArray = parseExerciseArray(answer);
        if (exerciseArray == null || exerciseArray.isEmpty()) {
            log.warn("未能从LLM响应中解析出习题，courseId={}, chapterId={}, answer={}", courseId, chapterId, answer);
            return 0;
        }

        // 8. 保存习题
        int savedCount = 0;
        for (int i = 0; i < exerciseArray.size(); i++) {
            try {
                JSONObject obj = exerciseArray.getJSONObject(i);
                if (obj == null) {
                    continue;
                }
                WrongQuestion wrongQuestion = buildWrongQuestion(obj, courseId, chapterId, operatorId);
                boolean saved = wrongQuestionService.save(wrongQuestion);
                if (saved) {
                    savedCount++;
                }
            } catch (Exception e) {
                log.warn("解析或保存第 {} 道习题失败: {}", i + 1, e.getMessage());
            }
        }
        log.info("AI习题生成完成，courseId={}, chapterId={}, 期望{}道，实际保存{}道",
                courseId, chapterId, questionCount, savedCount);
        return savedCount;
    }

    @Override
    public List<WrongQuestion> getCourseExercises(Long courseId, Long chapterId) {
        if (courseId == null || courseId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程ID不能为空");
        }
        QueryWrapper<WrongQuestion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("courseId", courseId);
        queryWrapper.eq("aiGenerated", 1);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.eq(chapterId != null && chapterId > 0, "chapterId", chapterId);
        queryWrapper.orderByDesc("createTime");
        return wrongQuestionService.list(queryWrapper);
    }

    /**
     * 构建课程内容文本
     */
    private String buildCourseContent(Course course, CourseChapter chapter, List<CourseSection> sections) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程名称：").append(course.getTitle()).append("\n");
        if (StrUtil.isNotBlank(course.getDescription())) {
            sb.append("课程描述：").append(course.getDescription()).append("\n");
        }
        if (StrUtil.isNotBlank(course.getRequirements())) {
            sb.append("学习要求：").append(course.getRequirements()).append("\n");
        }
        if (StrUtil.isNotBlank(course.getObjectives())) {
            sb.append("学习目标：").append(course.getObjectives()).append("\n");
        }
        if (chapter != null) {
            sb.append("章节名称：").append(chapter.getTitle()).append("\n");
            if (StrUtil.isNotBlank(chapter.getDescription())) {
                sb.append("章节描述：").append(chapter.getDescription()).append("\n");
            }
        } else {
            sb.append("章节范围：全部章节\n");
        }
        if (CollUtil.isNotEmpty(sections)) {
            sb.append("包含小节：\n");
            for (CourseSection section : sections) {
                sb.append("- ").append(section.getTitle());
                if (StrUtil.isNotBlank(section.getDescription())) {
                    sb.append("（").append(section.getDescription()).append("）");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 构建发送给 LLM 的 prompt
     */
    private String buildPrompt(Course course, CourseChapter chapter, List<CourseSection> sections, int questionCount) {
        String chapterTitle = chapter != null ? chapter.getTitle() : "全部章节";
        String chapterDesc = chapter != null && StrUtil.isNotBlank(chapter.getDescription())
                ? chapter.getDescription() : "无";
        String courseDesc = StrUtil.isNotBlank(course.getDescription()) ? course.getDescription() : "无";
        String sectionText = CollUtil.isEmpty(sections) ? "无"
                : sections.stream().map(s -> s.getTitle() + "（"
                        + (StrUtil.isNotBlank(s.getDescription()) ? s.getDescription() : "") + "）")
                .collect(Collectors.joining("、"));

        return "你是一个课程习题生成助手。请根据以下课程内容生成 " + questionCount + " 道练习题。\n\n"
                + "课程信息：\n"
                + "- 课程名称：" + course.getTitle() + "\n"
                + "- 课程描述：" + courseDesc + "\n"
                + "- 章节名称：" + chapterTitle + "\n"
                + "- 章节描述：" + chapterDesc + "\n"
                + "- 包含小节：" + sectionText + "\n\n"
                + "要求：\n"
                + "1. 生成选择题和简答题的混合\n"
                + "2. 选择题需包含4个选项（A/B/C/D）\n"
                + "3. 每题需包含正确答案和解析\n"
                + "4. 难度分为简单(1)、中等(2)、困难(3)\n\n"
                + "请以 JSON 数组格式返回，每个题目格式如下：\n"
                + "[\n"
                + "  {\n"
                + "    \"questionType\": \"choice\",\n"
                + "    \"questionContent\": \"题目内容\",\n"
                + "    \"options\": [\"A. 选项A\", \"B. 选项B\", \"C. 选项C\", \"D. 选项D\"],\n"
                + "    \"correctAnswer\": \"A\",\n"
                + "    \"analysis\": \"解析说明\",\n"
                + "    \"difficulty\": 2\n"
                + "  },\n"
                + "  {\n"
                + "    \"questionType\": \"short_answer\",\n"
                + "    \"questionContent\": \"简答题内容\",\n"
                + "    \"options\": [],\n"
                + "    \"correctAnswer\": \"参考答案\",\n"
                + "    \"analysis\": \"解析说明\",\n"
                + "    \"difficulty\": 1\n"
                + "  }\n"
                + "]\n\n"
                + "只返回 JSON 数组，不要添加其他文字。";
    }

    /**
     * 调用 Dify 获取 LLM 回答
     */
    private String callDify(AiAvatar aiAvatar, String prompt, String courseContent, Long operatorId) {
        DifyChatRequest chatRequest = new DifyChatRequest();
        chatRequest.setQuery(prompt);
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("course_content", courseContent);
        chatRequest.setInputs(inputs);
        chatRequest.setResponse_mode("blocking");
        chatRequest.setUser(difyConfig.getUserPrefix() + operatorId);
        chatRequest.setConversation_id(null);
        chatRequest.setAuto_generate_name(false);

        String url = aiAvatar.getBaseUrl() + "/chat-messages";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + aiAvatar.getAvatarAuth());

        String requestJson = JSONUtil.toJsonStr(chatRequest);

        Response response = null;
        try {
            response = okHttpUtils.postJson(url, requestJson, headers);
            if (!response.isSuccessful()) {
                String errBody = "";
                if (response.body() != null) {
                    errBody = response.body().string();
                }
                log.error("调用Dify生成习题失败: HTTP {}, body={}", response.code(), errBody);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "AI习题生成失败: HTTP " + response.code() + ", " + errBody);
            }
            if (response.body() == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI习题生成失败: Dify返回空响应");
            }
            String responseBody = response.body().string();
            JSONObject respObj = JSONUtil.parseObj(responseBody);
            String answer = respObj.getStr("answer");
            if (StrUtil.isBlank(answer)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI习题生成失败: Dify返回answer为空");
            }
            return answer;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用Dify生成习题异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI习题生成失败: " + e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * 从 LLM 回答中解析习题数组
     */
    private JSONArray parseExerciseArray(String answer) {
        if (StrUtil.isBlank(answer)) {
            return null;
        }
        // 1. 直接尝试解析为 JSON 数组
        try {
            return JSONUtil.parseArray(answer);
        } catch (Exception e) {
            log.debug("直接解析JSON数组失败，尝试正则提取");
        }
        // 2. 从文本中提取 JSON 数组部分
        String jsonStr = ReUtil.get("\\[[\\s\\S]*\\]", answer, 0);
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }
        try {
            return JSONUtil.parseArray(jsonStr);
        } catch (Exception e) {
            log.warn("从LLM响应中提取的JSON数组解析失败: {}", jsonStr);
            return null;
        }
    }

    /**
     * 根据解析出的习题 JSON 构建 WrongQuestion 实体
     */
    private WrongQuestion buildWrongQuestion(JSONObject obj, Long courseId, Long chapterId, Long operatorId) {
        WrongQuestion wrongQuestion = new WrongQuestion();
        wrongQuestion.setUserId(operatorId);
        wrongQuestion.setQuestionType(obj.getStr("questionType"));
        wrongQuestion.setQuestionContent(obj.getStr("questionContent"));
        wrongQuestion.setCorrectAnswer(obj.getStr("correctAnswer"));
        wrongQuestion.setAnalysis(obj.getStr("analysis"));

        // 选项（选择题）
        JSONArray optionsArr = obj.getJSONArray("options");
        if (optionsArr != null && !optionsArr.isEmpty()) {
            wrongQuestion.setOptions(JSONUtil.toJsonStr(optionsArr));
        }

        wrongQuestion.setSourceType("course");
        wrongQuestion.setSourceId(courseId);
        wrongQuestion.setCourseId(courseId);
        wrongQuestion.setChapterId(chapterId);

        Integer difficulty = obj.getInt("difficulty");
        wrongQuestion.setDifficulty(difficulty != null ? difficulty : 2);

        wrongQuestion.setAiGenerated(1);
        wrongQuestion.setMasteryLevel(0);
        wrongQuestion.setReviewCount(0);
        return wrongQuestion;
    }
}
