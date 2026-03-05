package com.aicreation.external;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;

import java.util.List;
import java.util.Objects;

/**
 * 通用的火山引擎 Ark Chat 调用客户端。
 * 内部统一管理API地址和模型配置，根据任务类型自动选择合适的模型。
 */
public interface VolcengineChatClient {

    /**
     * 聊天完成请求（使用默认模型）
     * @param content 消息内容
     * @return 响应内容列表
     */
    List<String> chatCompletions(Object content);

    /**
     * 流式聊天完成请求（使用默认模型）
     * @param content 消息内容
     * @param onChunk 每个数据块的回调函数
     */
    void streamChatCompletion(Object content, java.util.function.Consumer<String> onChunk);

    /**
     * 创建Responses API响应（用于上下文管理）
     * @param content 输入内容
     * @param previousResponseId 上一轮响应的ID，用于上下文关联
     * @param taskType 任务类型（title, content, reasoning等），用于选择合适的模型
     * @return ResponseObject 响应对象
     */
    ResponseObject createResponse(Object content, String previousResponseId, String taskType);

    /**
     * 流式Responses API响应
     * @param content 输入内容
     * @param previousResponseId 上一轮响应的ID，用于上下文关联
     * @param taskType 任务类型，用于选择合适的模型
     * @param eventHandler 事件处理器
     */
    void streamResponse(Object content, String previousResponseId, String taskType,
                       java.util.function.Consumer<Object> eventHandler);
}


