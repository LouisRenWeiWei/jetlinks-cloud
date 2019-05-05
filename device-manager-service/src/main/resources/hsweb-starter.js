//组件信息
var info = {
    groupId: "@groupId@",
    artifactId: "@artifactId@",
    version: "1.0.0",
    website: "https://github.com/jetlinks/jetlinks-cloud",
    author: "zhouhao",
    comment: "设备管理"
};

//版本更新信息
var versions = [
    // {
    //     version: "3.0.0",
    //     upgrade: function (context) {
    //         java.lang.System.out.println("更新到3.0.2了");
    //     }
    // }
];
var JDBCType = java.sql.JDBCType;

function install(context) {
    var database = context.database;
    database.createOrAlter("dev_classified_catalog")
        .addColumn().name("id").alias("id").comment("ID").jdbcType(java.sql.JDBCType.VARCHAR).length(32).primaryKey().commit()
        .addColumn().name("name").notNull().alias("name").comment("名称").jdbcType(java.sql.JDBCType.VARCHAR).length(64).commit()
        .addColumn().name("state").notNull().alias("state").comment("状态").jdbcType(java.sql.JDBCType.DECIMAL).length(4, 0).commit()
        .addColumn().name("describe").alias("describe").comment("说明").jdbcType(java.sql.JDBCType.VARCHAR).length(128).commit()
        .addColumn().name("parent_id").notNull().alias("parentId").comment("父级选项").jdbcType(java.sql.JDBCType.VARCHAR).length(32).commit()
        .addColumn().name("path").alias("path").comment("树编码").jdbcType(java.sql.JDBCType.VARCHAR).length(128).commit()
        .addColumn().name("search_code").alias("searchCode").comment("快速搜索码").jdbcType(java.sql.JDBCType.VARCHAR).length(128).commit()
        .addColumn().name("sort_index").notNull().alias("sortIndex").comment("排序索引").jdbcType(java.sql.JDBCType.DECIMAL).length(32, 0).commit()
        .addColumn().name("creator_id").alias("creatorId").comment("创建人ID").jdbcType(java.sql.JDBCType.VARCHAR).length(32).commit()
        .addColumn().name("create_time").alias("createTime").comment("创建时间").jdbcType(java.sql.JDBCType.DECIMAL).length(32, 0).commit()
        .addColumn().name("level_").alias("level").comment("树结构层级").jdbcType(java.sql.JDBCType.DECIMAL).length(32, 0).commit()
        .addColumn().name("properties").alias("properties").comment("其他自定义属性").jdbcType(java.sql.JDBCType.CLOB).commit()
        .index().name("idx_dcc_parent_id").column("parent_id").commit()
        .index().name("idx_dcc_creator_id").column("creator_id").commit()
        .index().name("idx_dcc_path").column("path").commit()
        .index().name("idx_dcc_search_code").column("search_code").commit()
        .comment("设备分类目录").commit();

    database.createOrAlter("dev_classified")
        .addColumn().name("id").alias("id").comment("ID").jdbcType(java.sql.JDBCType.VARCHAR).length(32).primaryKey().commit()
        .addColumn().name("name").notNull().alias("name").comment("名称").jdbcType(java.sql.JDBCType.VARCHAR).length(32).commit()
        .addColumn().name("default_metadata").alias("defaultMetadata").comment("默认元数据").clob().commit()
        .addColumn().name("catalog_id").notNull().alias("catalogId").comment("分类ID").varchar(32).commit()
        .addColumn().name("creator_id").notNull().alias("creatorId").comment("创建人id").jdbcType(java.sql.JDBCType.VARCHAR).length(32).commit()
        .addColumn().name("create_time").notNull().alias("createTime").comment("创建时间").jdbcType(java.sql.JDBCType.DECIMAL).length(32, 0).commit()
        .addColumn().name("search_code").alias("searchCode").comment("快速搜索码").jdbcType(java.sql.JDBCType.VARCHAR).length(128).commit()
        .addColumn().name("describe").alias("describe").comment("说明").jdbcType(java.sql.JDBCType.VARCHAR).length(256).commit()
        .index().name("idx_dc_catalog_id").column("catalog_id").commit()
        .index().name("idx_dc_creator_id").column("creator_id").commit()
        .index().name("idx_dc_search_code").column("search_code").commit()
        .comment("设备分类").commit();

    database.createOrAlter("dev_product")
        .addColumn().name("id").alias("id").comment("ID").jdbcType(java.sql.JDBCType.VARCHAR).length(32).primaryKey().commit()
        .addColumn().name("name").alias("name").notNull().comment("名称").jdbcType(java.sql.JDBCType.VARCHAR).length(32).commit()
        .addColumn().name("metadata").alias("metadata").comment("元数据").clob().commit()
        .addColumn().name("classified_id").alias("classifiedId").comment("分类ID").varchar(32).commit()

        .addColumn().name("project_id").alias("projectId").notNull().comment("项目ID").varchar(32).commit()
        .addColumn().name("project_name").alias("projectName").notNull().comment("项目名称").varchar(32).commit()

        .addColumn().name("message_protocol").alias("messageProtocol").comment("消息协议").varchar(32).commit()
        .addColumn().name("transport_protocol").alias("transport_protocol").comment("传输协议").varchar(32).commit()
        .addColumn().name("network_way").alias("networkWay").comment("入网方式").varchar(32).commit()
        .addColumn().name("product_type").alias("productType").comment("类型:DEVICE(设备),GATEWAY(网关)").varchar(32).commit()
        .addColumn().name("registry_way").alias("registryWay").comment("注册方式:AUTO(自动),MANUAL(手动)").varchar(32).commit()
        .addColumn().name("auth_way").alias("authWay").comment("认证方式:GATEWAY(网关认证),DEVICE(设备认证)").varchar(32).commit()
        .addColumn().name("sys_conf").alias("sysConfiguration").comment("系统配置").clob().commit()
        .addColumn().name("security_conf").alias("security").comment("安全配置").clob().commit()
        .addColumn().name("state").alias("state").comment("状态").jdbcType(java.sql.JDBCType.DECIMAL).length(4, 0).commit()

        .addColumn().name("creator_id").alias("creatorId").comment("创建人id").jdbcType(java.sql.JDBCType.VARCHAR).length(32).commit()
        .addColumn().name("creator_name").alias("creatorName").comment("创建人").jdbcType(java.sql.JDBCType.VARCHAR).length(128).commit()
        .addColumn().name("create_time").alias("createTime").comment("创建时间").jdbcType(java.sql.JDBCType.DECIMAL).length(32, 0).commit()
        .addColumn().name("device_features").alias("deviceFeatures").comment("设备可选功能").jdbcType(java.sql.JDBCType.DECIMAL).length(32, 0).commit()

        .addColumn().name("describe").alias("describe").comment("说明").jdbcType(java.sql.JDBCType.VARCHAR).length(256).commit()
        .index().name("idx_prod_project_id").column("project_id").commit()
        .index().name("idx_prod_state").column("state").commit()
        .index().name("idx_prod_creator_id").column("creator_id").commit()
        .comment("设备产品").commit();

    database.createOrAlter("dev_device_instance")
        .addColumn().name("id").alias("id").comment("ID").jdbcType(java.sql.JDBCType.VARCHAR).length(32).primaryKey().commit()
        .addColumn().name("name").alias("name").notNull().comment("名称").jdbcType(java.sql.JDBCType.VARCHAR).length(32).commit()

        .addColumn().name("describe").alias("describe").comment("说明").jdbcType(java.sql.JDBCType.VARCHAR).length(256).commit()
        .addColumn().name("product_id").alias("productId").notNull().comment("产品ID").varchar(32).commit()
        .addColumn().name("product_name").alias("productName").notNull().comment("产品名称").varchar(32).commit()

        .addColumn().name("derive_metadata").alias("deriveMetadata").comment("默认元数据").clob().commit()
        .addColumn().name("sys_conf").alias("sysConfiguration").comment("系统配置").clob().commit()
        .addColumn().name("security_conf").alias("security").comment("安全配置").clob().commit()
        .addColumn().name("state").alias("state").comment("状态").notNull().jdbcType(java.sql.JDBCType.DECIMAL).length(4, 0).commit()

        .addColumn().name("creator_id").alias("creatorId").notNull().comment("创建人id").jdbcType(java.sql.JDBCType.VARCHAR).length(32).commit()
        .addColumn().name("creator_name").alias("creatorName").comment("创建人").jdbcType(java.sql.JDBCType.VARCHAR).length(128).commit()
        .addColumn().name("create_time").alias("createTime").notNull().comment("创建时间").jdbcType(java.sql.JDBCType.DECIMAL).length(32, 0).commit()
        .addColumn().name("registry_time").alias("registryTime").comment("注册时间").jdbcType(java.sql.JDBCType.DECIMAL).length(32, 0).commit()
        .addColumn().name("properties").alias("properties").comment("其他属性").jdbcType(java.sql.JDBCType.CLOB).commit()
        .addColumn().name("device_features").alias("deviceFeatures").comment("设备可选功能").jdbcType(java.sql.JDBCType.DECIMAL).length(32, 0).commit()

        .index().name("idx_dci_product_id").column("product_id").commit()
        .index().name("idx_dci_creator_id").column("creator_id").commit()
        .index().name("idx_dci_state").column("state").commit()
        .comment("设备分类").commit();

}

//设置依赖
dependency.setup(info)
    .onInstall(install)
    .onUpgrade(function (context) { //更新时执行
        var upgrader = context.upgrader;
        upgrader.filter(versions)
            .upgrade(function (newVer) {
                newVer.upgrade(context);
            });
    })
    .onUninstall(function (context) { //卸载时执行

    });