package com.aicreation.external.impl;

import com.aicreation.external.VolcengineChatClient;
import com.aicreation.external.config.VolcengineProperties;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionResult;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.model.responses.request.ResponsesInput;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 火山引擎 Ark Chat 客户端实现
 * 内部统一管理API配置，不需要外部传入URL和模型参数
 *
 * @author AI-Creation Team
 * @date 2026/03/05
 * @version 1.0.0
 */
@Slf4j
@Service
public class VolcengineChatClientImpl implements VolcengineChatClient {

    @Autowired
    private VolcengineProperties volcengineProperties;

    @Override
    public List<String> chatCompletions(Object content) {
        String url = com.aicreation.external.config.AIConfig.getBaseUrl();
        String model = com.aicreation.external.config.AIConfig.DEFAULT_MODEL;

        // 创建 ArkService 实例
        ArkService arkService = ArkService.builder()
                .apiKey(volcengineProperties.getApiKey())
                .baseUrl(url)
                .timeout(java.time.Duration.ofMinutes(30))
                .build();

        try {
            // 初始化消息列表
            List<ChatMessage> chatMessages = new ArrayList<>();

            // 创建用户消息
            ChatMessage userMessage = ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(content.toString())
                    .build();

            chatMessages.add(userMessage);

            // 构建Chat Completions请求
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(chatMessages)
                    .build();

            // 发送请求并处理响应
            ChatCompletionResult result = arkService.createChatCompletion(request);
            List<String> contents = new ArrayList<>();
            if (result.getChoices() != null) {
                for (ChatCompletionChoice choice : result.getChoices()) {
                    if (choice.getMessage() != null) {
                        contents.add((String) choice.getMessage().getContent());
                    }
                }
            }

            return contents;

        } catch (Exception e) {
            log.error("Chat Completions API调用失败", e);
            throw new RuntimeException("Chat Completions API调用失败: " + e.getMessage(), e);
        } finally {
            arkService.shutdownExecutor();
        }
    }

    @Override
    public void streamChatCompletion(Object content, Consumer<String> onChunk) {
        String url = com.aicreation.external.config.AIConfig.getBaseUrl();
        String model = com.aicreation.external.config.AIConfig.DEFAULT_MODEL;

        // 创建 ArkService 实例
        ArkService arkService = ArkService.builder()
                .apiKey(volcengineProperties.getApiKey())
                .baseUrl(url)
                .timeout(java.time.Duration.ofMinutes(30))
                .build();

        try {
            // 初始化消息列表
            List<ChatMessage> chatMessages = new ArrayList<>();
            chatMessages.add(ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(content.toString())
                    .build());

            // 构建请求
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(chatMessages)
                    .stream(true)
                    .build();

            // 流式处理响应
            arkService.streamChatCompletion(request)
                    .blockingForEach(chunk -> {
                        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                            ChatCompletionChoice choice = chunk.getChoices().get(0);
                            if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                                onChunk.accept((String) choice.getMessage().getContent());
                            }
                        }
                    });

        } catch (Exception e) {
            log.error("流式Chat Completions API调用失败", e);
            throw new RuntimeException("流式Chat Completions API调用失败: " + e.getMessage(), e);
        } finally {
            arkService.shutdownExecutor();
        }
    }

    @Override
    public ResponseObject createResponse(Object content, String previousResponseId, String taskType) {
        String url = com.aicreation.external.config.AIConfig.getBaseUrl();
        String model = com.aicreation.external.config.AIConfig.getRecommendedModel(taskType);

        // 创建 ArkService 实例
        ArkService arkService = ArkService.builder()
                .apiKey(volcengineProperties.getApiKey())
                .baseUrl(url)
                .timeout(java.time.Duration.ofMinutes(30))
                .build();

        try {
            // 构建Responses API请求
            CreateResponsesRequest request = CreateResponsesRequest.builder()
                    .model(model)
                    .input(ResponsesInput.builder().stringValue(content.toString()).build())
                    .stream(false)
                    .build();

            // 如果有上一轮的response_id，则关联上下文
            if (previousResponseId != null && !previousResponseId.trim().isEmpty()) {
                request.setPreviousResponseId(previousResponseId);
            }

            // 发送请求并返回响应
            return arkService.createResponse(request);

        } catch (Exception e) {
            log.error("Responses API调用失败", e);

            // 针对不同错误类型提供更友好的错误信息
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("AccountOverdueError") || errorMessage.contains("overdue balance")) {
                    throw new RuntimeException("AI服务账户余额不足，请联系管理员充值", e);
                } else if (errorMessage.contains("403") || errorMessage.contains("Forbidden")) {
                    throw new RuntimeException("AI服务访问被拒绝，请检查账户权限", e);
                } else if (errorMessage.contains("429") || errorMessage.contains("Too Many Requests")) {
                    throw new RuntimeException("AI服务请求过于频繁，请稍后再试", e);
                } else if (errorMessage.contains("500") || errorMessage.contains("Internal Server Error")) {
                    throw new RuntimeException("AI服务暂时不可用，请稍后再试", e);
                }
            }

            throw new RuntimeException("AI服务调用失败: " + errorMessage, e);
        } finally {
            // 关闭服务执行器
            arkService.shutdownExecutor();
        }
    }

    @Override
    public void streamResponse(Object content, String previousResponseId, String taskType,
                              Consumer<Object> eventHandler) {
        String url = com.aicreation.external.config.AIConfig.getBaseUrl();
        String model = com.aicreation.external.config.AIConfig.getRecommendedModel(taskType);

        // 创建 ArkService 实例
        ArkService arkService = ArkService.builder()
                .apiKey(volcengineProperties.getApiKey())
                .baseUrl(url)
                .timeout(java.time.Duration.ofMinutes(30))
                .build();

        try {
            // 构建Responses API请求
            CreateResponsesRequest request = CreateResponsesRequest.builder()
                    .model(model)
                    .input(ResponsesInput.builder().stringValue(content.toString()).build())
                    .stream(true)
                    .build();

            // 如果有上一轮的response_id，则关联上下文
            if (previousResponseId != null && !previousResponseId.trim().isEmpty()) {
                request.setPreviousResponseId(previousResponseId);
            }

            // 使用流式方式处理响应
            arkService.streamResponse(request)
                    .blockingForEach(event -> {
                        try {
                            eventHandler.accept(event);
                        } catch (Exception e) {
                            log.error("处理流式响应事件失败", e);
                        }
                    });

        } catch (Exception e) {
            log.error("流式Responses API调用失败", e);
            throw new RuntimeException("流式Responses API调用失败: " + e.getMessage(), e);
        } finally {
            // 关闭服务执行器
            arkService.shutdownExecutor();
        }
    }
}


