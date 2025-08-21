package com.aicreation.common;

/**
 * 音色常量
 * 
 * @author AI-Creation Team
 * @date 2024/01/01
 * @version 1.0.0
 */
public class VoiceTone {

    /**
     * Alex音色
     */
    public static final String ALEX = "alex";

    /**
     * Anna音色
     */
    public static final String ANNA = "anna";

    /**
     * 获取音色描述
     * 
     * @param voiceTone 音色值
     * @return 音色描述
     */
    public static String getVoiceToneDescription(String voiceTone) {
        if (voiceTone == null) {
            return "未知";
        }
        switch (voiceTone) {
            case ALEX:
                return "Alex";
            case ANNA:
                return "Anna";
            default:
                return "未知";
        }
    }

    /**
     * 判断是否为有效的音色值
     * 
     * @param voiceTone 音色值
     * @return 是否有效
     */
    public static boolean isValidVoiceTone(String voiceTone) {
        return voiceTone != null && (voiceTone.equals(ALEX) || voiceTone.equals(ANNA));
    }

    /**
     * 获取所有可用的音色值
     * 
     * @return 音色值数组
     */
    public static String[] getAllVoiceTones() {
        return new String[]{ALEX, ANNA};
    }
}
