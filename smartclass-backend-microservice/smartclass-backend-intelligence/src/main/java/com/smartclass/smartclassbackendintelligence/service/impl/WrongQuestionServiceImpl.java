package com.smartclass.smartclassbackendintelligence.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartclass.smartclassbackendintelligence.mapper.WrongQuestionMapper;
import com.smartclass.smartclassbackendintelligence.service.WrongQuestionService;
import com.smartclass.smartclassbackendmodel.model.entity.WrongQuestion;
import org.springframework.stereotype.Service;

/**
 * @description 针对表【wrong_question(错题本/AI习题)】的数据库操作Service实现
 */
@Service
public class WrongQuestionServiceImpl extends ServiceImpl<WrongQuestionMapper, WrongQuestion>
        implements WrongQuestionService {

}
