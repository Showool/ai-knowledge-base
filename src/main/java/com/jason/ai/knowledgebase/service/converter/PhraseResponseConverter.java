package com.jason.ai.knowledgebase.service.converter;

import org.mapstruct.Mapper;

import com.jason.ai.knowledgebase.model.entity.MeaninglessPhrase;
import com.jason.ai.knowledgebase.model.response.PhraseResponses.PhraseView;

/** 无意义短语响应转换器。 */
@Mapper(config = MapStructConfiguration.class)
public interface PhraseResponseConverter {

    /** 将短语实体转换为管理视图。 */
    PhraseView toView(MeaninglessPhrase phrase);
}
