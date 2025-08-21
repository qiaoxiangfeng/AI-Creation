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


    void chatCompletions(String url, String model, Object content);
}


