package com.jason.ai.knowledgebase.repository.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jason.ai.knowledgebase.model.entity.MeaninglessPhrase;

public interface MeaninglessPhraseMapper extends BaseMapper<MeaninglessPhrase> {
    @Select("SELECT COUNT(1) FROM ai_meaningless_phrase WHERE phrase = #{phrase} AND deleted = 0")
    int countByPhrase(@Param("phrase") String phrase);
}
