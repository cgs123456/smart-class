package com.cgs.smartclassbackendmodel.esdao;

import com.cgs.smartclassbackendmodel.model.dto.course.CourseKnowledgeEsDTO;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 课程知识 ES 数据访问层
 */
public interface CourseKnowledgeEsDao extends ElasticsearchRepository<CourseKnowledgeEsDTO, Long> {

    /**
     * 根据课程ID查询知识文档
     */
    List<CourseKnowledgeEsDTO> findByCourseId(Long courseId);

    /**
     * 根据课程ID删除知识文档
     */
    void deleteByCourseId(Long courseId);
}
