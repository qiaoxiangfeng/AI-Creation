package com.aicreation.external;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;

import java.util.List;
import java.util.Objects;

/**
 * 通用的火山引擎 Ark Chat 调用客户端。
 * 通过参数传入 apiKey、url、model，使用 SDK 原生对象请求与返回。
 */
public interface VolcengineChatClient {


    List<String> chatCompletions(String url, String model, Object content);

    /**
     * 流式聊天完成请求
     * @param url 基础URL
     * @param model 模型ID
     * @param content 消息内容
     * @param onChunk 每个数据块的回调函数
     */
    void streamChatCompletion(String url, String model, Object content,
                            java.util.function.Consumer<String> onChunk);
}


