package com.aicreation.converter;

import com.aicreation.entity.bo.ArticleBo;
import com.aicreation.entity.dto.ArticleCreateReqDto;
import com.aicreation.entity.dto.ArticleRespDto;
import com.aicreation.entity.dto.ArticleUpdateReqDto;
import com.aicreation.entity.po.Article;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 * 文章对象转换器
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ArticleConverter {

	/**
	 * 提供 MapStruct 的默认实例，便于在非 Spring 上下文中直接使用
	 */
	ArticleConverter INSTANCE = Mappers.getMapper(ArticleConverter.class);

    /**
     * 创建请求DTO转换为业务对象
     */
    ArticleBo toArticleBo(ArticleCreateReqDto request);

    /**
     * 更新请求DTO转换为业务对象
     */
    ArticleBo toArticleBo(ArticleUpdateReqDto request);

    /**
     * 持久化对象转换为响应DTO
     */
    ArticleRespDto toArticleRespDto(Article article);

    /**
     * 业务对象转换为持久化对象
     */
    Article toArticle(ArticleBo articleBo);

    /**
     * 更新请求DTO转换为持久化对象
     */
    @Mapping(source = "articleId", target = "id")
    Article toArticle(ArticleUpdateReqDto request);
}
