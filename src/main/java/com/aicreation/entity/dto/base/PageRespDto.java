package com.aicreation.entity.dto.base;

import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

/**
 * @author lifeng
 * @Description: 分页通用返回对象
 * @date 2022/4/6 13:51
 */
@Getter
@Setter
@Schema(description = "分页通用返回参数")
@AllArgsConstructor
@NoArgsConstructor
public class PageRespDto<T> extends BaseDto {

    @Schema(description = "当前页")
    private int pageNo;

    @Schema(description = "每页的数量")
    private int pageSize;

    @Schema(description = "当前页的数量")
    private int size;

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "总页数")
    private int pages;

    @Schema(description = "结果集")
    private List<T> list;

    /**
     * 泛型转换
     */
    @SuppressWarnings("unchecked")
    public <R> PageRespDto<R> convert(Function<? super T, ? extends R> mapper) {
        List<R> collect = this.getList().stream().map(mapper).collect(toList());
        ((PageRespDto<R>) this).setList(collect);
        return (PageRespDto<R>)this;
    }

    public static <T> PageRespDto<T> of(PageInfo<T> pageInfo) {
        return new PageRespDto<T>(pageInfo.getPageNum(), pageInfo.getPageSize(), pageInfo.getSize(), pageInfo.getTotal(), pageInfo.getPages(), pageInfo.getList());
    }
}
