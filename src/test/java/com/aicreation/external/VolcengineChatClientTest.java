package com.aicreation.external;

import com.aicreation.AiCreationApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = AiCreationApplication.class)
class VolcengineChatClientTest {

    @Autowired
    private DouBaoClient douBaoClient;

    @Test
    void test1() {
        String content = "请随机生成一篇儿童故事的标题与简介，文章标题不超过20个字符，且需要保证标题有意义。"
        +"文章简介不超过200个字符，不包含故事细节，只是介绍一个大的范围框架，可以通过简介大概知道是个什么故事，"
                +"故事主角的形象刻画描述，刻画描述不超过300个字符，包含所有主角的特征"
        +"文章内容不超过10000个字符，情节需要完整，要包含故事的开始、发展、高潮、结局。"
        +"用以下json格式返回：{\n" +
                "  \"articleName\": \"文章标题\",\n" +
                "  \"articleOutline\": \"文章简介。\",\n" +
                "  \"imageDesc\": \"形象描述\",\n" +
                "  \"articleContent\": \"文章内容\"\n" +
                "}";
        douBaoClient.chatCompletions(content);
    }
}


