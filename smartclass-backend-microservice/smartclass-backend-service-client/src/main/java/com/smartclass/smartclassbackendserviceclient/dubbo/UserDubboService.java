package com.smartclass.smartclassbackendserviceclient.dubbo;

import com.smartclass.smartclassbackendmodel.model.entity.User;
import com.smartclass.smartclassbackendmodel.model.vo.UserVO;

import java.util.Collection;
import java.util.List;

/**
 * 用户 Dubbo 服务接口
 * 
 * 定义内部 RPC 方法，替代原 Feign UserInnerController
 * 
 * 迁移对照:
 * - UserFeignClient.getById          -> getById
 * - UserFeignClient.getUserVO        -> getUserVO
 * - UserFeignClient.getUserVOById    -> getUserVOById
 * - UserFeignClient.listByIds        -> listByIds
 * - UserFeignClient.updateById       -> updateById
 */
public interface UserDubboService {

    /**
     * 根据ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    User getById(Long id);

    /**
     * 获取脱敏的用户信息列表
     *
     * @param userList 用户列表
     * @return 脱敏后的用户信息列表
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 根据用户ID获取脱敏的用户信息
     *
     * @param userId 用户ID
     * @return 脱敏后的用户信息
     */
    UserVO getUserVOById(Long userId);

    /**
     * 获取单个脱敏的用户信息 (default 方法在消费端执行)
     *
     * @param user 用户信息
     * @return 脱敏后的用户信息
     */
    default UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        List<UserVO> list = getUserVO(java.util.Collections.singletonList(user));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 根据用户ID集合批量获取用户信息
     *
     * @param userIds 用户ID集合
     * @return 用户信息列表
     */
    List<User> listByIds(Collection<Long> userIds);

    /**
     * 更新用户信息
     *
     * @param user 用户
     * @return 是否成功
     */
    boolean updateById(User user);
}