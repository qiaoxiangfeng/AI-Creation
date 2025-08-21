package com.aicreation.converter;

import com.aicreation.entity.bo.ArticleBo;
import com.aicreation.entity.dto.ArticleCreateReqDto;
import com.aicreation.entity.dto.ArticleRespDto;
import com.aicreation.entity.dto.ArticleUpdateReqDto;
import com.aicreation.entity.po.Article;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-21T13:41:16+0800",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.50.v20250729-0351, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class ArticleConverterImpl implements ArticleConverter {

    @Override
    public ArticleBo toArticleBo(ArticleCreateReqDto request) {
        if ( request == null ) {
            return null;
        }

        ArticleBo articleBo = new ArticleBo();

        articleBo.setArticleName( request.getArticleName() );
        articleBo.setArticleOutline( request.getArticleOutline() );
        articleBo.setImageDesc( request.getImageDesc() );
        articleBo.setVoiceTone( request.getVoiceTone() );
        articleBo.setVoiceLink( request.getVoiceLink() );
        articleBo.setVoiceFilePath( request.getVoiceFilePath() );
        articleBo.setVideoLink( request.getVideoLink() );
        articleBo.setVideoFilePath( request.getVideoFilePath() );
        articleBo.setPublishStatus( request.getPublishStatus() );

        return articleBo;
    }

    @Override
    public ArticleBo toArticleBo(ArticleUpdateReqDto request) {
        if ( request == null ) {
            return null;
        }

        ArticleBo articleBo = new ArticleBo();

        articleBo.setArticleName( request.getArticleName() );
        articleBo.setArticleOutline( request.getArticleOutline() );
        articleBo.setImageDesc( request.getImageDesc() );
        articleBo.setVoiceTone( request.getVoiceTone() );
        articleBo.setVoiceLink( request.getVoiceLink() );
        articleBo.setVoiceFilePath( request.getVoiceFilePath() );
        articleBo.setVideoLink( request.getVideoLink() );
        articleBo.setVideoFilePath( request.getVideoFilePath() );
        articleBo.setPublishStatus( request.getPublishStatus() );

        return articleBo;
    }

    @Override
    public ArticleRespDto toArticleRespDto(Article article) {
        if ( article == null ) {
            return null;
        }

        ArticleRespDto articleRespDto = new ArticleRespDto();

        articleRespDto.setId( article.getId() );
        articleRespDto.setArticleName( article.getArticleName() );
        articleRespDto.setArticleOutline( article.getArticleOutline() );
        articleRespDto.setImageDesc( article.getImageDesc() );
        articleRespDto.setArticleContent( article.getArticleContent() );
        articleRespDto.setVoiceTone( article.getVoiceTone() );
        articleRespDto.setVoiceLink( article.getVoiceLink() );
        articleRespDto.setVoiceFilePath( article.getVoiceFilePath() );
        articleRespDto.setVideoLink( article.getVideoLink() );
        articleRespDto.setVideoFilePath( article.getVideoFilePath() );
        articleRespDto.setPublishStatus( article.getPublishStatus() );
        articleRespDto.setResState( article.getResState() );
        articleRespDto.setCreateTime( article.getCreateTime() );
        articleRespDto.setUpdateTime( article.getUpdateTime() );

        return articleRespDto;
    }

    @Override
    public Article toArticle(ArticleBo articleBo) {
        if ( articleBo == null ) {
            return null;
        }

        Article article = new Article();

        article.setArticleName( articleBo.getArticleName() );
        article.setArticleOutline( articleBo.getArticleOutline() );
        article.setImageDesc( articleBo.getImageDesc() );
        article.setVoiceTone( articleBo.getVoiceTone() );
        article.setVoiceLink( articleBo.getVoiceLink() );
        article.setVoiceFilePath( articleBo.getVoiceFilePath() );
        article.setVideoLink( articleBo.getVideoLink() );
        article.setVideoFilePath( articleBo.getVideoFilePath() );
        article.setPublishStatus( articleBo.getPublishStatus() );
        article.setResState( articleBo.getResState() );
        article.setCreateTime( articleBo.getCreateTime() );
        article.setUpdateTime( articleBo.getUpdateTime() );

        return article;
    }

    @Override
    public Article toArticle(ArticleUpdateReqDto request) {
        if ( request == null ) {
            return null;
        }

        Article article = new Article();

        article.setId( request.getArticleId() );
        article.setArticleName( request.getArticleName() );
        article.setArticleOutline( request.getArticleOutline() );
        article.setImageDesc( request.getImageDesc() );
        article.setVoiceTone( request.getVoiceTone() );
        article.setVoiceLink( request.getVoiceLink() );
        article.setVoiceFilePath( request.getVoiceFilePath() );
        article.setVideoLink( request.getVideoLink() );
        article.setVideoFilePath( request.getVideoFilePath() );
        article.setPublishStatus( request.getPublishStatus() );

        return article;
    }
}
