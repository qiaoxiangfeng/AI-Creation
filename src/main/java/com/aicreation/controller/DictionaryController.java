package com.aicreation.controller;

import com.aicreation.entity.dto.*;
import com.aicreation.entity.dto.base.BaseResponse;
import com.aicreation.entity.dto.base.PageRespDto;
import com.aicreation.service.IDictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 字典管理控制器
 *
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
@Tag(name = "字典管理", description = "字典相关接口")
@RestController
@RequestMapping("/dictionaries")
public class DictionaryController {

    @Autowired
    private IDictionaryService dictionaryService;

    @Operation(summary = "根据ID查询字典", description = "根据字典ID查询字典详细信息")
    @GetMapping("/{id}")
    public BaseResponse<DictionaryRespDto> getDictionaryById(
            @Parameter(description = "字典ID") @PathVariable Long id) {
        DictionaryQueryReqDto request = new DictionaryQueryReqDto();
        request.setId(id);
        DictionaryRespDto dictionary = dictionaryService.getDictionaryById(request);
        return BaseResponse.success(dictionary);
    }

    @Operation(summary = "根据字典键查询字典列表", description = "根据字典键查询字典值列表")
    @GetMapping("/key/{dictKey}")
    public BaseResponse<List<DictionaryRespDto>> getDictionaryByKey(
            @Parameter(description = "字典键") @PathVariable String dictKey) {
        List<DictionaryRespDto> dictionaries = dictionaryService.getDictionaryByKey(dictKey);
        return BaseResponse.success(dictionaries);
    }

    @Operation(summary = "创建字典", description = "创建新的字典")
    @PostMapping
    public BaseResponse<Long> createDictionary(
            @Parameter(description = "字典创建请求") @Valid @RequestBody DictionaryCreateReqDto request) {
        Long dictionaryId = dictionaryService.createDictionary(request);
        return BaseResponse.success(dictionaryId);
    }

    @Operation(summary = "更新字典", description = "更新字典信息")
    @PutMapping
    public BaseResponse<Boolean> updateDictionary(
            @Parameter(description = "字典更新请求") @Valid @RequestBody DictionaryUpdateReqDto request) {
        Boolean result = dictionaryService.updateDictionary(request);
        return BaseResponse.success(result);
    }

    @Operation(summary = "删除字典", description = "删除指定字典")
    @DeleteMapping
    public BaseResponse<Boolean> deleteDictionary(
            @Parameter(description = "字典删除请求") @Valid @RequestBody DictionaryDeleteReqDto request) {
        Boolean result = dictionaryService.deleteDictionary(request);
        return BaseResponse.success(result);
    }

    @Operation(summary = "查询字典列表", description = "分页查询字典列表，支持按字典键和值筛选")
    @PostMapping("/list")
    public BaseResponse<PageRespDto<DictionaryListRespDto>> getDictionaryList(
            @Parameter(description = "字典列表查询请求") @Valid @RequestBody DictionaryListReqDto request) {
        PageRespDto<DictionaryListRespDto> page = dictionaryService.getDictionaryList(request);
        return BaseResponse.success(page);
    }
}