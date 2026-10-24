package com.smartclass.smartclassbackendcommon.tenant;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/**
 * 租户数据访问层
 */
@Mapper
public interface TenantMapper extends BaseMapper<TenantInfo> {

    /**
     * 根据租户编码查询
     */
    @Select("SELECT * FROM tenant_info WHERE code = #{code} AND is_delete = 0")
    Optional<TenantInfo> findByCode(String code);

    /**
     * 根据租户 ID 查询
     */
    @Select("SELECT * FROM tenant_info WHERE id = #{id} AND is_delete = 0")
    Optional<TenantInfo> findById(String id);

    /**
     * 查询所有租户 (用于数据源初始化)
     */
    @Select("SELECT * FROM tenant_info WHERE is_delete = 0")
    List<TenantInfo> findAll();

    /**
     * 根据域名查询
     */
    @Select("SELECT * FROM tenant_info WHERE domain = #{domain} AND is_delete = 0")
    Optional<TenantInfo> findByDomain(String domain);

    /**
     * 查询所有激活租户
     */
    @Select("SELECT * FROM tenant_info WHERE status = 'ACTIVE' AND is_delete = 0")
    List<TenantInfo> findAllActive();

    /**
     * 根据管理员 ID 查询租户
     */
    @Select("SELECT * FROM tenant_info WHERE admin_id = #{adminId} AND is_delete = 0")
    Optional<TenantInfo> findByAdminId(String adminId);
}