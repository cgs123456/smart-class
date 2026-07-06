package com.cgs.smartclass.model.dto.course;

import com.cgs.smartclass.model.entity.Course;
import com.cgs.smartclass.model.entity.CourseChapter;
import com.cgs.smartclass.model.entity.CourseSection;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

/**
 * 课程知识 ES 包装类
 * 用于 RAG 检索，聚合课程、章节、小节的内容
 */
@Document(indexName = "course_knowledge")
@Data
public class CourseKnowledgeEsDTO implements Serializable {

    /**
     * 课程级别 DTO 的 ID 偏移量
     */
    private static final long CHAPTER_ID_OFFSET = 10_000_000L;

    /**
     * 小节级别 DTO 的 ID 偏移量
     */
    private static final long SECTION_ID_OFFSET = 20_000_000L;

    /**
     * id（按类型偏移保证全局唯一）
     */
    @Id
    private Long id;

    /**
     * 课程ID
     */
    @Field(type = FieldType.Long)
    private Long courseId;

    /**
     * 章节ID（课程级别 DTO 为 null）
     */
    @Field(type = FieldType.Long)
    private Long chapterId;

    /**
     * 小节ID（课程/章节级别 DTO 为 null）
     */
    @Field(type = FieldType.Long)
    private Long sectionId;

    /**
     * 标题
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /**
     * 内容（聚合后的全文）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    /**
     * 内容类型：course / chapter / section
     */
    @Field(type = FieldType.Keyword)
    private String contentType;

    /**
     * 向量嵌入
     */
    @Field(type = FieldType.Dense_Vector, dims = 1024)
    private float[] embedding;

    private static final long serialVersionUID = 1L;

    /**
     * 从 Course 实体构建，聚合 title + description + requirements + objectives + targetAudience
     */
    public static CourseKnowledgeEsDTO buildFromCourse(Course course) {
        if (course == null || course.getId() == null) {
            return null;
        }
        CourseKnowledgeEsDTO dto = new CourseKnowledgeEsDTO();
        dto.setId(course.getId());
        dto.setCourseId(course.getId());
        dto.setChapterId(null);
        dto.setSectionId(null);
        dto.setTitle(course.getTitle());
        StringBuilder content = new StringBuilder();
        appendIfNotBlank(content, course.getDescription());
        appendIfNotBlank(content, course.getRequirements());
        appendIfNotBlank(content, course.getObjectives());
        appendIfNotBlank(content, course.getTargetAudience());
        dto.setContent(content.toString());
        dto.setContentType("course");
        return dto;
    }

    /**
     * 从 CourseChapter 实体构建，聚合 title + description
     */
    public static CourseKnowledgeEsDTO buildFromChapter(CourseChapter chapter) {
        if (chapter == null || chapter.getId() == null) {
            return null;
        }
        CourseKnowledgeEsDTO dto = new CourseKnowledgeEsDTO();
        dto.setId(chapter.getId() + CHAPTER_ID_OFFSET);
        dto.setCourseId(chapter.getCourseId());
        dto.setChapterId(chapter.getId());
        dto.setSectionId(null);
        dto.setTitle(chapter.getTitle());
        StringBuilder content = new StringBuilder();
        appendIfNotBlank(content, chapter.getDescription());
        dto.setContent(content.toString());
        dto.setContentType("chapter");
        return dto;
    }

    /**
     * 从 CourseSection 实体构建，聚合 title + description
     */
    public static CourseKnowledgeEsDTO buildFromSection(CourseSection section) {
        if (section == null || section.getId() == null) {
            return null;
        }
        CourseKnowledgeEsDTO dto = new CourseKnowledgeEsDTO();
        dto.setId(section.getId() + SECTION_ID_OFFSET);
        dto.setCourseId(section.getCourseId());
        dto.setChapterId(section.getChapterId());
        dto.setSectionId(section.getId());
        dto.setTitle(section.getTitle());
        StringBuilder content = new StringBuilder();
        appendIfNotBlank(content, section.getDescription());
        dto.setContent(content.toString());
        dto.setContentType("section");
        return dto;
    }

    private static void appendIfNotBlank(StringBuilder sb, String text) {
        if (text != null && !text.trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(text);
        }
    }
}
