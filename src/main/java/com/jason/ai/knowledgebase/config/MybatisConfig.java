package com.jason.ai.knowledgebase.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.annotation.DbType;

/** 配置 MyBatis 分页与 UTC 时间戳自动填充。 */
@Configuration
public class MybatisConfig {

    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    MetaObjectHandler instantMetaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
                strictInsertFill(metaObject, "createTime", Instant.class, now);
                strictInsertFill(metaObject, "updateTime", Instant.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updateTime", Instant.class,
                        Instant.now().truncatedTo(ChronoUnit.MILLIS));
            }
        };
    }
}
