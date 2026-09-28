package com.greengrid.admin.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置项，绑定 application.yml 中的 greengrid.jwt.*。
 */
@Data
@Component
@ConfigurationProperties(prefix = "greengrid.jwt")
public class JwtProperties {

    /** 签名密钥，至少 32 字节（256 位），HS256 算法要求 */
    private String secret;

    /** 令牌有效期（分钟） */
    private long expireMinutes = 120;

    /** 鉴权请求头名称 */
    private String header = "Authorization";

    /** 令牌前缀 */
    private String prefix = "Bearer ";
}
