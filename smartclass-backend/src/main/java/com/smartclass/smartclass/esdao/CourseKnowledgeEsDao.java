package com.smartclass.smartclass.esdao;

import com.smartclass.smartclass.model.dto.course.CourseKnowledgeEsDTO;
import java.util.List;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 课程知识 ES 操作
 */
public interface CourseKnowledgeEsDao extends ElasticsearchRepository<CourseKnowledgeEsDTO, Long> {

    /**
     * 根据课程ID查询
     */
    List<CourseKnowledgeEsDTO> findByCourseId(Long courseId);

    /**
     * 根据课程ID删除
     */
    void deleteByCourseId(Long courseId);
}
