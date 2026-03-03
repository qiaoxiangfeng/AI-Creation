package com.aicreation.external;

import com.aicreation.AiCreationApplication;
import com.aicreation.external.dto.TtsRequest;
import com.aicreation.external.dto.TtsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;

/**
 * 火山引擎语音合成客户端测试（基于官方文档）
 * 
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@SpringBootTest(classes = AiCreationApplication.class)
public class VolcengineTtsClientTest {
    
    @Autowired
    private VolcengineTtsClient volcengineTtsClient;
    
    @Test
    public void testSynthesizeText() {
        // 测试HTTP方式合成语音
        String text = "你好，我是豆包语音助手，很高兴认识你。";
        String voiceType = "zh_female_cancan_mars_bigtts";
        
        TtsResponse response = volcengineTtsClient.synthesizeText(text, voiceType);
        
        System.out.println("合成结果:");
        System.out.println("请求ID: " + response.getReqId());
        System.out.println("状态码: " + response.getCode());
        System.out.println("消息: " + response.getMessage());
        System.out.println("音频数据长度: " + (response.getData() != null ? response.getData().length() : 0));
        if (response.getAddition() != null) {
            System.out.println("音频时长: " + response.getAddition().getDuration() + "ms");
        }
    }
    
    @Test
    public void testSynthesizeTextAsync() throws Exception {
        // 测试异步合成语音
        String text = "这是一个异步语音合成测试。";
        String voiceType = "zh_female_cancan_mars_bigtts";
        
        CompletableFuture<TtsResponse> future = volcengineTtsClient.synthesizeTextAsync(text, voiceType);
        
        TtsResponse response = future.get();
        
        System.out.println("异步合成结果:");
        System.out.println("请求ID: " + response.getReqId());
        System.out.println("状态码: " + response.getCode());
        System.out.println("消息: " + response.getMessage());
    }
    
    @Test
    public void testSynthesizeTextStream() throws Exception {
        // 测试WebSocket流式合成语音
        String text = "这是流式语音合成测试，可以实时接收音频数据。";
        String voiceType = "zh_female_cancan_mars_bigtts";
        
        System.out.println("开始流式合成测试...");
        
        volcengineTtsClient.synthesizeTextStream(text, voiceType, 
            response -> {
                System.out.println("收到音频数据，序号: " + response.getSequence() + 
                    ", 是否最后一段: " + (response.getSequence() != null && response.getSequence() < 0));
                System.out.println("音频数据长度: " + (response.getData() != null ? response.getData().length() : 0));
                
                if (response.getSequence() != null && response.getSequence() < 0) {
                    System.out.println("流式合成完成");
                }
            },
            error -> {
                System.err.println("合成错误: " + error);
            }
        );
        
        // 等待一段时间让流式合成完成
        Thread.sleep(10000);
    }
    
    @Test
    public void testBuildRequest() {
        // 测试构建请求对象
        String text = "测试构建请求对象";
        String voiceType = "zh_female_cancan_mars_bigtts";
        
        TtsRequest request = volcengineTtsClient.buildDefaultRequest(text, voiceType);
        
        System.out.println("请求对象构建完成:");
        System.out.println("应用ID: " + request.getApp().getAppId());
        System.out.println("音色类型: " + request.getAudio().getVoiceType());
        System.out.println("文本: " + request.getRequest().getText());
        System.out.println("操作类型: " + request.getRequest().getOperation());
        
        // 测试构建流式请求
        TtsRequest streamRequest = volcengineTtsClient.buildStreamRequest(text, voiceType);
        System.out.println("流式请求操作类型: " + streamRequest.getRequest().getOperation());
    }
    
    @Test
    public void testCustomRequest() {
        // 测试自定义请求
        String text = "自定义请求测试";
        String voiceType = "zh_female_cancan_mars_bigtts";
        
        TtsRequest request = new TtsRequest();
        
        // 应用配置
        TtsRequest.App app = new TtsRequest.App();
        app.setAppId("test_app_id");
        app.setToken("test_token");
        app.setCluster("volcano_tts");
        request.setApp(app);
        
        // 用户配置
        TtsRequest.User user = new TtsRequest.User();
        user.setUid("test_user");
        request.setUser(user);
        
        // 音频配置
        TtsRequest.Audio audio = new TtsRequest.Audio();
        audio.setVoiceType(voiceType);
        audio.setEncoding("mp3");
        audio.setSpeedRatio(1.0);
        audio.setRate(24000);
        audio.setBitrate(160);
        audio.setEmotion("happy");
        audio.setEnableEmotion(true);
        audio.setEmotionScale(4.0);
        request.setAudio(audio);
        
        // 请求配置
        TtsRequest.Request requestConfig = new TtsRequest.Request();
        requestConfig.setReqId("test_req_id");
        requestConfig.setText(text);
        requestConfig.setOperation("query");
        requestConfig.setModel("seed-tts-1.1");
        requestConfig.setWithTimestamp(1);
        request.setRequest(requestConfig);
        
        System.out.println("自定义请求对象构建完成:");
        System.out.println("应用ID: " + request.getApp().getAppId());
        System.out.println("音色类型: " + request.getAudio().getVoiceType());
        System.out.println("情感: " + request.getAudio().getEmotion());
        System.out.println("情感强度: " + request.getAudio().getEmotionScale());
        System.out.println("模型: " + request.getRequest().getModel());
        System.out.println("时间戳: " + request.getRequest().getWithTimestamp());
    }
}
