package com.cgs.smartclassbackendmodel.model.dto.course;

import com.cgs.smartclassbackendmodel.model.entity.Course;
import com.cgs.smartclassbackendmodel.model.entity.CourseChapter;
import com.cgs.smartclassbackendmodel.model.entity.CourseSection;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

/**
 * 课程知识 ES 文档
 * 用于 RAG 检索的课程知识向量化存储
 */
@Document(indexName = "course_knowledge")
@Data
public class CourseKnowledgeEsDTO implements Serializable {

    /**
     * 文档ID（使用实体自身ID）
     */
    @Id
    private Long id;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 小节ID
     */
    private Long sectionId;

    /**
     * 标题
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /**
     * 内容
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    /**
     * 内容类型：course-课程，chapter-章节，section-小节
     */
    @Field(type = FieldType.Keyword)
    private String contentType;

    /**
     * 向量嵌入
     */
    private float[] embedding;

    private static final long serialVersionUID = 1L;

    /**
     * 从课程对象构建 ES 文档
     */
    public static CourseKnowledgeEsDTO buildFromCourse(Course course) {
        CourseKnowledgeEsDTO dto = new CourseKnowledgeEsDTO();
        dto.setId(course.getId());
        dto.setCourseId(course.getId());
        dto.setChapterId(null);
        dto.setSectionId(null);
        dto.setTitle(course.getTitle());
        StringBuilder sb = new StringBuilder();
        if (course.getDescription() != null) {
            sb.append(course.getDescription()).append("\n");
        }
        if (course.getRequirements() != null) {
            sb.append("学习要求：").append(course.getRequirements()).append("\n");
        }
        if (course.getObjectives() != null) {
            sb.append("学习目标：").append(course.getObjectives()).append("\n");
        }
        if (course.getTargetAudience() != null) {
            sb.append("目标受众：").append(course.getTargetAudience());
        }
        dto.setContent(sb.toString());
        dto.setContentType("course");
        return dto;
    }

    /**
     * 从章节对象构建 ES 文档
     */
    public static CourseKnowledgeEsDTO buildFromChapter(CourseChapter chapter) {
        CourseKnowledgeEsDTO dto = new CourseKnowledgeEsDTO();
        dto.setId(chapter.getId());
        dto.setCourseId(chapter.getCourseId());
        dto.setChapterId(chapter.getId());
        dto.setSectionId(null);
        dto.setTitle(chapter.getTitle());
        dto.setContent(chapter.getDescription() != null ? chapter.getDescription() : "");
        dto.setContentType("chapter");
        return dto;
    }

    /**
     * 从小节对象构建 ES 文档
     */
    public static CourseKnowledgeEsDTO buildFromSection(CourseSection section) {
        CourseKnowledgeEsDTO dto = new CourseKnowledgeEsDTO();
        dto.setId(section.getId());
        dto.setCourseId(section.getCourseId());
        dto.setChapterId(section.getChapterId());
        dto.setSectionId(section.getId());
        dto.setTitle(section.getTitle());
        dto.setContent(section.getDescription() != null ? section.getDescription() : "");
        dto.setContentType("section");
        return dto;
    }
}
