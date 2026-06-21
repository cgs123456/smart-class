package com.cgs.smartclass.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cgs.smartclass.model.dto.ebook.EbookAddRequest;
import com.cgs.smartclass.model.dto.ebook.EbookQueryRequest;
import com.cgs.smartclass.model.entity.Ebook;
import com.cgs.smartclass.model.vo.EbookVO;

/**
 * 电子书服务
 */
public interface EbookService extends IService<Ebook> {

    /**
     * 分页查询电子书列表
     *
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    Page<EbookVO> listEbooks(EbookQueryRequest queryRequest);

    /**
     * 根据ID获取电子书
     *
     * @param id 电子书ID
     * @return 电子书视图
     */
    EbookVO getEbookById(Long id);

    /**
     * 添加电子书（管理员）
     *
     * @param addRequest 添加请求
     * @return 新增ID
     */
    Long addEbook(EbookAddRequest addRequest);

    /**
     * 更新电子书（管理员）
     *
     * @param ebook 电子书实体
     * @return 是否成功
     */
    boolean updateEbook(Ebook ebook);

    /**
     * 删除电子书（管理员）
     *
     * @param id 电子书ID
     * @return 是否成功
     */
    boolean deleteEbook(Long id);
}
