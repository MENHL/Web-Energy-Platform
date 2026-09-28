-- =====================================================================
-- GreenGrid 建表脚本（MySQL 8，utf8mb4）
-- 说明：此脚本由 spring.sql.init 在启动时自动执行（dev 环境 mode=always）。
--       采用 DROP + CREATE 的「幂等重建」策略，仅用于开发环境；
--       生产环境 sql.init.mode=never，改用 Flyway 或手工执行。
-- =====================================================================

SET NAMES utf8mb4;

-- 按外键依赖顺序反向删除
DROP TABLE IF EXISTS device_metric;
DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS report;
DROP TABLE IF EXISTS stock_order;
DROP TABLE IF EXISTS material;
DROP TABLE IF EXISTS device;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS sys_user;

-- ---------------------------------------------------------------------
-- 用户表
-- ---------------------------------------------------------------------
CREATE TABLE sys_user (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    username      VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password      VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    name          VARCHAR(50)  NOT NULL COMMENT '姓名',
    role          VARCHAR(20)  NOT NULL COMMENT '角色编码：super/ops/warehouse/project/guest',
    role_name     VARCHAR(20)  NOT NULL COMMENT '角色名称',
    dept          VARCHAR(50)  NOT NULL COMMENT '所属部门',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
    last_login_at DATETIME     NULL COMMENT '最近登录时间',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 / 1 已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username, deleted),
    KEY idx_role (role),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- ---------------------------------------------------------------------
-- 项目表
-- ---------------------------------------------------------------------
CREATE TABLE project (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_code VARCHAR(32)  NOT NULL COMMENT '项目编号',
    name         VARCHAR(100) NOT NULL COMMENT '项目名称',
    region       VARCHAR(16)  NOT NULL COMMENT '区域：华东/华北/华南/华中/西北/西南/东北',
    manager      VARCHAR(50)  NOT NULL COMMENT '负责人',
    status       VARCHAR(16)  NOT NULL DEFAULT '进行中' COMMENT '状态：进行中/已暂停/已完成',
    progress     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '进度 0-100',
    start_date   DATE         NULL COMMENT '计划开始日期',
    end_date     DATE         NULL COMMENT '计划结束日期',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_code (project_code, deleted),
    KEY idx_status (status),
    KEY idx_region (region),
    KEY idx_updated_at (updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目表';

-- ---------------------------------------------------------------------
-- 设备表
-- ---------------------------------------------------------------------
CREATE TABLE device (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_code    VARCHAR(32)  NOT NULL COMMENT '设备编号',
    name           VARCHAR(100) NOT NULL COMMENT '设备名称',
    type           VARCHAR(20)  NOT NULL COMMENT '类型：逆变器/风电机组/储能系统/光伏阵列/输配电',
    project_id     BIGINT UNSIGNED NULL COMMENT '所属项目 id（关联 project.id）',
    status         VARCHAR(16)  NOT NULL DEFAULT '在线' COMMENT '状态：在线/离线/故障/维护中',
    install_date   DATE         NULL COMMENT '安装日期',
    last_update_at DATETIME     NULL COMMENT '最后上报/维护时间',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_code (device_code, deleted),
    KEY idx_project_id (project_id),
    KEY idx_status (status),
    KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备表';

-- ---------------------------------------------------------------------
-- 物料表
-- ---------------------------------------------------------------------
CREATE TABLE material (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    material_code VARCHAR(32)  NOT NULL COMMENT '物料编码',
    name          VARCHAR(100) NOT NULL COMMENT '物料名称',
    category      VARCHAR(20)  NOT NULL COMMENT '分类：电芯电池/光伏组件/电气元件/结构件辅材',
    spec          VARCHAR(100) NULL COMMENT '规格型号',
    unit          VARCHAR(16)  NOT NULL DEFAULT '件' COMMENT '计量单位',
    stock         INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前库存',
    safety_stock  INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '安全库存',
    status        VARCHAR(16)  NOT NULL DEFAULT '正常' COMMENT '状态：正常/低库存/缺货（由服务层重算）',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_material_code (material_code, deleted),
    KEY idx_category (category),
    KEY idx_status (status),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料表';

-- ---------------------------------------------------------------------
-- 出入库单表
-- ---------------------------------------------------------------------
CREATE TABLE stock_order (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no      VARCHAR(32)  NOT NULL COMMENT '单据号（入库 RK 开头 / 出库 CK 开头）',
    order_type    VARCHAR(10)  NOT NULL COMMENT 'INBOUND 入库 / OUTBOUND 出库',
    material_id   BIGINT UNSIGNED NOT NULL COMMENT '物料 id',
    material_name VARCHAR(100) NOT NULL COMMENT '物料名称（冗余，便于展示与追溯）',
    quantity      INT UNSIGNED NOT NULL COMMENT '数量',
    target        VARCHAR(100) NOT NULL COMMENT '入库=仓库名 / 出库=去向',
    operator      VARCHAR(50)  NOT NULL COMMENT '经手人',
    order_time    DATETIME     NOT NULL COMMENT '单据时间',
    remark        VARCHAR(255) NULL COMMENT '备注',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_material_id (material_id, order_time),
    KEY idx_type_time (order_type, order_time),
    KEY idx_order_time (order_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出入库单表';

-- ---------------------------------------------------------------------
-- 报表表
-- ---------------------------------------------------------------------
CREATE TABLE report (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    type       VARCHAR(20)  NOT NULL COMMENT '类型：运营汇总/库存分析/设备分析/物料分析/报表导出',
    name       VARCHAR(100) NOT NULL COMMENT '报表名称',
    status     VARCHAR(16)  NOT NULL DEFAULT '生成中' COMMENT '状态：生成中/已生成/已取消',
    creator    VARCHAR(50)  NOT NULL COMMENT '创建人',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_type_status (type, status),
    KEY idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报表表';

-- ---------------------------------------------------------------------
-- 操作日志表
-- ---------------------------------------------------------------------
CREATE TABLE sys_operation_log (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT UNSIGNED NULL COMMENT '操作人 id（系统操作为 null）',
    operator   VARCHAR(50)  NOT NULL COMMENT '操作人姓名（或"系统"）',
    module     VARCHAR(30)  NOT NULL COMMENT '模块',
    action     VARCHAR(16)  NOT NULL COMMENT '动作：新增/更新/删除/维护/登录',
    content    VARCHAR(500) NOT NULL COMMENT '操作内容',
    ip         VARCHAR(45)  NULL COMMENT '客户端 IP（兼容 IPv6）',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_created_at (created_at DESC),
    KEY idx_module_action (module, action),
    KEY idx_operator (operator)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';

-- ---------------------------------------------------------------------
-- 设备监控指标表（一期建表，暂不写入数据）
-- ---------------------------------------------------------------------
CREATE TABLE device_metric (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_id   BIGINT UNSIGNED NOT NULL COMMENT '设备 id',
    collect_at  DATETIME     NOT NULL COMMENT '采集时间',
    load_rate   DECIMAL(5,2) NULL COMMENT '负载率 %',
    temperature DECIMAL(5,2) NULL COMMENT '温度 ℃',
    PRIMARY KEY (id),
    KEY idx_device_time (device_id, collect_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备监控指标表';
