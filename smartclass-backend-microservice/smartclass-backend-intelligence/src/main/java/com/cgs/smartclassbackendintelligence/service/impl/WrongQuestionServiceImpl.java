package com.cgs.smartclassbackendintelligence.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclassbackendintelligence.mapper.WrongQuestionMapper;
import com.cgs.smartclassbackendintelligence.service.WrongQuestionService;
import com.cgs.smartclassbackendmodel.model.entity.WrongQuestion;
import org.springframework.stereotype.Service;

/**
 * @author cgs
 * @description 针对表【wrong_question(错题本/AI习题)】的数据库操作Service实现
 */
@Service
public class WrongQuestionServiceImpl extends ServiceImpl<WrongQuestionMapper, WrongQuestion>
        implements WrongQuestionService {

}
