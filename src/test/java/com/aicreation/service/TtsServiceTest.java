package com.aicreation.service;

import com.aicreation.AiCreationApplication;
import com.aicreation.controller.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 语音合成服务正向用例测试（每个接口一个）
 *
 * @author qiaoxiangfeng
 * @date 2025/9/5
 */
@SpringBootTest(classes = AiCreationApplication.class)
public class TtsServiceTest {

    @Autowired
    private TtsService ttsService;

    @Test
    public void testSynthesizeText_ok() {
        TtsSynthesizeReqDto request = new TtsSynthesizeReqDto();
        request.setText("测试语音合成 - 正向");
        request.setVoiceType("zh_female_cancan_mars_bigtts");
        request.setSaveFile(true);

        TtsSynthesizeRespDto response = ttsService.synthesizeText(request);

        assertNotNull(response);
        assertNotNull(response.getReqId());
        assertEquals(3000, response.getCode());
        assertEquals("Success", response.getMessage());
    }

    @Test
    public void testSynthesizeTextAsync_ok() {
        TtsAsyncReqDto request = new TtsAsyncReqDto();
        request.setText("测试异步语音合成 - 正向");
        request.setVoiceType("zh_female_cancan_mars_bigtts");

        TtsAsyncRespDto response = ttsService.synthesizeTextAsync(request);

        assertNotNull(response);
        assertNotNull(response.getTaskId());
        assertEquals("SUBMITTED", response.getStatus());
        assertEquals("异步合成任务已提交", response.getMessage());
    }

    @Test
    public void testSynthesizeTextStream_ok() {
        TtsStreamReqDto request = new TtsStreamReqDto();
        request.setText("测试流式语音合成 - 正向");
        request.setVoiceType("zh_female_cancan_mars_bigtts");
        request.setSaveFile(false);

        SseEmitter emitter = ttsService.synthesizeTextStream(request);

        assertNotNull(emitter);
    }

    @Test
    public void testGetAudioFileInfo_ok() {
        TtsSynthesizeReqDto request = new TtsSynthesizeReqDto();
        request.setText("测试获取文件信息 - 正向");
        request.setVoiceType("zh_female_cancan_mars_bigtts");
        request.setSaveFile(true);

        TtsSynthesizeRespDto synthesizeResponse = ttsService.synthesizeText(request);

        TtsAudioInfoRespDto fileInfo = ttsService.getAudioFileInfo(synthesizeResponse.getReqId());

        assertNotNull(fileInfo);
        assertEquals(synthesizeResponse.getReqId(), fileInfo.getReqId());
        assertTrue(fileInfo.getExists());
        assertTrue(fileInfo.getFileSize() > 0);
    }

    @Test
    public void testGetAudioFile_ok() {
        TtsSynthesizeReqDto request = new TtsSynthesizeReqDto();
        request.setText("测试下载文件 - 正向");
        request.setVoiceType("zh_female_cancan_mars_bigtts");
        request.setSaveFile(true);

        TtsSynthesizeRespDto synthesizeResponse = ttsService.synthesizeText(request);

        ResponseEntity<org.springframework.core.io.Resource> fileResponse = ttsService.getAudioFile(synthesizeResponse.getReqId());

        assertNotNull(fileResponse);
        assertNotNull(fileResponse.getBody());
        assertTrue(fileResponse.getBody().exists());
    }

    @Test
    public void testDeleteAudioFile_ok() {
        TtsSynthesizeReqDto request = new TtsSynthesizeReqDto();
        request.setText("测试删除文件 - 正向");
        request.setVoiceType("zh_female_cancan_mars_bigtts");
        request.setSaveFile(true);

        TtsSynthesizeRespDto synthesizeResponse = ttsService.synthesizeText(request);

        TtsDeleteRespDto deleteResponse = ttsService.deleteAudioFile(synthesizeResponse.getReqId());

        assertNotNull(deleteResponse);
        assertEquals(synthesizeResponse.getReqId(), deleteResponse.getReqId());
        assertTrue(deleteResponse.getSuccess());
    }
}
