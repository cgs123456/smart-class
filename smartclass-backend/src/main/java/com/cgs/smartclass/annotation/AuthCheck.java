package com.cgs.smartclass.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验
*/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须有某个角色
     *
     * @return
     */
    String mustRole() default "";

    /**
     * 必须有其中任一角色（多角色校验，向后兼容 mustRole）
     * 当 mustRoles 不为空时优先使用多角色校验，忽略 mustRole
     *
     * @return
     */
    String[] mustRoles() default {};

}

