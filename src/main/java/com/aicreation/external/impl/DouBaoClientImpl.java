package com.aicreation.external.impl;

import com.aicreation.external.DouBaoClient;
import com.aicreation.external.VolcengineChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @DESCRITION DouBaoClientImpl
 * @AUTHOR qiaoxiangfeng
 * @DATA 2025/8/20 17:07
 **/
@Service
public class DouBaoClientImpl implements DouBaoClient {


    @Autowired
    private VolcengineChatClient volcengineChatClient;


    private final String DOUBAO_SEED_1_6_URL="https://ark.cn-beijing.volces.com/api/v3/chat/completions";
    private final String DOUBAO_SEED_1_6__MODEL="doubao-seed-1-6-250615";

    @Override
    public void chatCompletions(String content) {

        volcengineChatClient.chatCompletions(DOUBAO_SEED_1_6_URL,DOUBAO_SEED_1_6__MODEL,content);
    }
}
