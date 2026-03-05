package com.aicreation.external.impl;

import com.aicreation.external.DouBaoClient;
import com.aicreation.external.VolcengineChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @DESCRITION DouBaoChatClientImpl
 * @AUTHOR qiaoxiangfeng
 * @DATA 2025/8/20 17:07
 **/
@Service
public class DouBaoChatClientImpl implements DouBaoClient {


    @Autowired
    private VolcengineChatClient volcengineChatClient;



    @Override
    public void chatCompletions(String content) {
        List<String> contents = volcengineChatClient.chatCompletions(content);
        // 这里可以添加对返回结果的处理逻辑
        if (contents != null && !contents.isEmpty()) {
            System.out.println("AI Response: " + contents.get(0));
        }
    }
}
