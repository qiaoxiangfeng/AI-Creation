package com.aicreation.util;

import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * Volcengine Responses API 响应内容提取工具
 * 用于正确提取不同类型的响应内容
 *
 * @author AI-Creation Team
 * @date 2026/03/05
 * @version 1.0.0
 */
@Slf4j
public class ResponseContentExtractor {

    /**
     * 从 ResponseObject 中提取实际的文本内容
     *
     * @param response Responses API 的响应对象
     * @return 提取的文本内容
     */
    public static String extractContent(ResponseObject response) {
        if (response == null || response.getOutput() == null || response.getOutput().isEmpty()) {
            throw new RuntimeException("响应对象为空或不包含输出内容");
        }

        // 遍历output，获取type为message的内容
        StringBuilder messageContent = new StringBuilder();
        boolean foundMessage = false;

        for (Object item : response.getOutput()) {
            try {
                // 使用反射获取type和content
                var typeField = item.getClass().getMethod("getType");
                String type = (String) typeField.invoke(item);

                if ("message".equals(type)) {
                    var contentField = item.getClass().getMethod("getContent");
                    Object content = contentField.invoke(item);

                    if (content != null) {
                        String contentStr = extractTextFromContent(content);
                        messageContent.append(contentStr);
                        foundMessage = true;
                        log.debug("提取到message类型内容，长度: {}", contentStr.length());
                    } else {
                        log.debug("message类型的content为null");
                    }
                } else if ("reasoning".equals(type)) {
                    log.debug("跳过reasoning类型的条目");
                }
            } catch (Exception e) {
                log.warn("解析output条目失败: {}", e.getMessage());
            }
        }

        if (!foundMessage || messageContent.length() == 0) {
            log.warn("响应中未找到message类型的条目内容，尝试降级处理");
            // 降级处理：如果没有找到message类型，尝试旧的处理方式
            return fallbackExtractContent(response);
        }

        String result = messageContent.toString();
        log.info("成功提取message内容，总长度: {}", result.length());
        return result;
    }

    /**
     * 降级内容提取（兼容旧的处理方式）
     */
    private static String fallbackExtractContent(ResponseObject response) {
        Object outputItem = response.getOutput().get(0);
        log.info("降级处理 - 响应对象类型: {}, 类名: {}", outputItem.getClass().getSimpleName(), outputItem.getClass().getName());

        // 处理 ItemReasoning 类型响应（推理模型）
        if (isItemReasoning(outputItem)) {
            log.info("检测到 ItemReasoning 类型响应，使用降级处理");
            return extractFromItemReasoning(outputItem);
        }

        // 处理 ItemText 类型响应（普通文本模型）
        if (isItemText(outputItem)) {
            log.info("检测到 ItemText 类型响应");
            return extractFromItemText(outputItem);
        }

        // 尝试其他可能的响应类型
        String content = tryExtractFromOtherTypes(outputItem);
        if (content != null) {
            log.info("从其他类型提取到内容，长度: {}", content.length());
            return content;
        }

        // 处理其他类型，尝试直接转换为字符串
        log.warn("未知的响应类型: {}, 尝试直接转换为字符串", outputItem.getClass().getName());
        String result = outputItem.toString();
        log.info("直接转换为字符串的结果长度: {}", result != null ? result.length() : 0);
        if (result != null && result.length() > 0) {
            log.info("直接转换为字符串的结果(前200字符): {}", result.substring(0, Math.min(200, result.length())));
        }
        return result;
    }

    /**
     * 检查对象是否为 ItemReasoning 类型
     */
    private static boolean isItemReasoning(Object obj) {
        return obj.getClass().getName().contains("ItemReasoning");
    }

    /**
     * 检查对象是否为 ItemText 类型
     */
    private static boolean isItemText(Object obj) {
        return obj.getClass().getName().contains("ItemText");
    }

    /**
     * 从 ItemReasoning 对象中提取内容
     */
    private static String extractFromItemReasoning(Object reasoningObj) {
        try {
            log.info("开始解析 ItemReasoning 对象");

            // 使用反射获取 summary 字段
            var summaryField = reasoningObj.getClass().getMethod("getSummary");
            @SuppressWarnings("unchecked")
            var summaryList = (java.util.List<Object>) summaryField.invoke(reasoningObj);

            log.info("获取到 summary 列表，包含 {} 个元素", summaryList != null ? summaryList.size() : 0);

            if (summaryList == null || summaryList.isEmpty()) {
                throw new RuntimeException("ItemReasoning 对象不包含 summary 内容");
            }

            StringBuilder content = new StringBuilder();
            for (Object summaryPart : summaryList) {
                log.debug("处理 summary part，类型: {}", summaryPart.getClass().getName());

                // 获取 summary part 的文本内容
                if (summaryPart.getClass().getName().contains("ReasoningSummaryPart")) {
                    var textField = summaryPart.getClass().getMethod("getText");
                    String text = (String) textField.invoke(summaryPart);
                    log.debug("提取到的文本: {}", text);
                    if (text != null) {
                        content.append(text);
                    }
                }
            }

            String result = content.toString();
            log.debug("最终提取的内容长度: {}", result.length());

            if (content.length() == 0) {
                throw new RuntimeException("无法从 ItemReasoning 对象中提取文本内容");
            }

            return result;

        } catch (Exception e) {
            log.error("解析 ItemReasoning 对象失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析 ItemReasoning 对象失败: " + e.getMessage(), e);
        }
    }

    /**
     * 尝试从其他可能的响应类型中提取内容
     */
    private static String tryExtractFromOtherTypes(Object obj) {
        try {
            String className = obj.getClass().getName();
            log.debug("尝试从类型 {} 中提取内容", className);

            // 尝试常见的字段名
            String[] possibleFields = {"content", "text", "result", "output", "data", "value"};

            for (String fieldName : possibleFields) {
                try {
                    var field = obj.getClass().getMethod("get" + capitalize(fieldName));
                    Object value = field.invoke(obj);
                    if (value != null) {
                        String content = value.toString();
                        log.debug("从字段 {} 提取到内容: {}", fieldName, content.substring(0, Math.min(100, content.length())));
                        return content;
                    }
                } catch (Exception e) {
                    // 继续尝试下一个字段
                    log.debug("字段 {} 不存在或无法访问", fieldName);
                }
            }

            // 如果对象本身就是字符串，直接返回
            if (obj instanceof String) {
                log.debug("对象本身就是字符串");
                return (String) obj;
            }

            // 尝试调用 toString() 方法
            String toStringResult = obj.toString();
            if (toStringResult != null && !toStringResult.startsWith(obj.getClass().getName() + "@")) {
                log.debug("toString() 返回有效内容");
                return toStringResult;
            }

        } catch (Exception e) {
            log.debug("尝试提取其他类型内容失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 从content对象中提取text字段的内容
     */
    private static String extractTextFromContent(Object content) {
        if (content == null) {
            return "";
        }

        // 如果content是数组或列表，遍历处理每个元素
        if (content.getClass().isArray() || content instanceof java.util.List) {
            StringBuilder result = new StringBuilder();
            java.util.List<?> contentList;

            if (content.getClass().isArray()) {
                contentList = java.util.Arrays.asList((Object[]) content);
            } else {
                contentList = (java.util.List<?>) content;
            }

            log.debug("content是数组/列表，包含 {} 个元素", contentList.size());

            for (Object item : contentList) {
                String itemText = extractTextFromSingleItem(item);
                if (!itemText.isEmpty()) {
                    result.append(itemText);
                }
            }

            String finalResult = result.toString();
            log.debug("从数组/列表中提取的总内容长度: {}", finalResult.length());
            return finalResult;
        } else {
            // 处理单个对象
            return extractTextFromSingleItem(content);
        }
    }

    /**
     * 从单个content项目中提取text内容
     */
    private static String extractTextFromSingleItem(Object item) {
        if (item == null) {
            return "";
        }

        try {
            // 尝试获取text字段
            var textField = item.getClass().getMethod("getText");
            Object text = textField.invoke(item);

            if (text != null) {
                String textStr = text.toString();
                log.debug("成功从单个项目提取text内容，长度: {}", textStr.length());
                return textStr;
            }
        } catch (Exception e) {
            log.debug("项目没有text字段，尝试直接转换为字符串，类型: {}", item.getClass().getName());
        }

        // 如果没有text字段，直接转换为字符串
        String itemStr = item.toString();
        log.debug("直接转换项目为字符串，长度: {}", itemStr.length());
        return itemStr;
    }

    /**
     * 首字母大写
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 从 ItemText 对象中提取内容
     */
    private static String extractFromItemText(Object textObj) {
        try {
            // 使用反射获取 content 字段
            var contentField = textObj.getClass().getMethod("getContent");
            Object content = contentField.invoke(textObj);

            if (content == null) {
                throw new RuntimeException("ItemText 对象不包含 content 内容");
            }

            return content.toString();

        } catch (Exception e) {
            log.error("解析 ItemText 对象失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析 ItemText 对象失败: " + e.getMessage(), e);
        }
    }
}