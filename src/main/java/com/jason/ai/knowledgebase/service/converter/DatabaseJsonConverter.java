package com.jason.ai.knowledgebase.service.converter;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import com.jason.ai.knowledgebase.common.util.JsonCodec;

import lombok.RequiredArgsConstructor;

/** 为 MapStruct 提供数据库 JSON 字段转换。 */
@Component
@RequiredArgsConstructor
public class DatabaseJsonConverter {

    private final JsonCodec jsonCodec;

    /** 将数据库 metadata JSON 解析为对外对象结构。 */
    @Named("readMetadata")
    public Object readMetadata(String metadata) {
        return jsonCodec.readObject(metadata);
    }
}
