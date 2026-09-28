package com.greengrid.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置：分页插件（依赖 mybatis-plus-jsqlparser）。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor();
        pagination.setDbType(DbType.MYSQL);
        // 单页最大 100 条，防止 pageSize 被恶意传大值
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);

        return interceptor;
    }
}
