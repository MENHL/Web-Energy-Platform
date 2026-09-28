package com.greengrid.admin.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 工具类。兼容直接访问与反向代理（Nginx 等）场景。
 */
public final class IpUtil {

    private static final String UNKNOWN = "unknown";

    private IpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !UNKNOWN.equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能是 "client, proxy1, proxy2"，取第一个
                int comma = ip.indexOf(',');
                return (comma > 0 ? ip.substring(0, comma) : ip).trim();
            }
        }
        return request.getRemoteAddr();
    }
}
