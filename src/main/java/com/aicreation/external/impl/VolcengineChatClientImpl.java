package com.aicreation.external.impl;

import com.aicreation.external.VolcengineChatClient;
import com.aicreation.external.config.VolcengineProperties;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class VolcengineChatClientImpl implements VolcengineChatClient {


    @Autowired
    private VolcengineProperties volcengineProperties;

    @Override
    public List<String> chatCompletions(String url,String model,Object content) {

        // 创建 ArkService 实例
        ArkService arkService = ArkService.builder().apiKey(volcengineProperties.getApiKey()).baseUrl(url).build();

        // 初始化消息列表
        List<ChatMessage> chatMessages = new ArrayList<>();

        // 创建用户消息
        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER) // 设置消息角色为用户
                //.content("你好") // 设置消息内容
                .build();

        userMessage.setContent(content);
        // 将用户消息添加到消息列表
        chatMessages.add(userMessage);

        // 创建聊天完成请求
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
               // .model("doubao-seed-1-6-250615") // 按需替换 Model ID
                .model(model)
                .messages(chatMessages) // 设置消息列表
                .build();

        // 发送聊天完成请求并打印响应
        try {
            List<String> contents=new ArrayList<>();
            arkService.createChatCompletion(chatCompletionRequest)
                    .getChoices()
                    .forEach(choice -> {
                        contents.add(choice.getMessage().getContent().toString());
                    });
            return contents;
        } catch (Exception e) {
            log.error("请求失败", e);
        } finally {
            // 关闭服务执行器
            arkService.shutdownExecutor();
        }
        return Collections.emptyList();
    }



}


