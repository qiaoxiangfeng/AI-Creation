package com.aicreation.entity.dto.base;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.Serializable;

/**
 * @author ：李国鹏
 * @Date ：Created in 3:17 PM 2020/3/9
 * @Description：
 * @Modified By：
 * @Version: 1.0.0
 */
public class BaseDto  implements Serializable{

    @Override
    public String toString() {
        //默认toString 方法，可以将子类成员变量打印出来
        return ReflectionToStringBuilder.toString(this);
    }
}
