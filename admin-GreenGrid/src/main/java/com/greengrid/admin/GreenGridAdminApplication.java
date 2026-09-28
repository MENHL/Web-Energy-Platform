package com.greengrid.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GreenGrid 新能源企业运营数据可视化平台 —— 后端启动类。
 *
 * @author greengrid
 */
@SpringBootApplication
@MapperScan("com.greengrid.admin.modules")
public class GreenGridAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreenGridAdminApplication.class, args);
    }
}
