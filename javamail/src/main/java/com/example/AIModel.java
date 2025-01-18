package com.example;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;

import java.util.Arrays;

public class AIModel {

    // API密钥和模型名称
    private static final String API_KEY = "xxx";//替换为你的API-key，严肃应用时不推荐这么写，可以参考官方代码
    private static final String MODEL_NAME = "qwen-plus";

    // 生成内容的方法
    public GenerationResult generateContent(String prompt) throws Exception {
        try {
            // 创建Generation对象，用于调用生成API
            Generation gen = new Generation();

            // 构建系统消息，设置助手角色和初始提示
            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("You are a helpful assistant.")
                    .build();

            // 构建用户消息，包含用户输入的提示
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build();

            // 构建生成参数，包括API密钥、模型名称、消息列表和结果格式
            GenerationParam param = GenerationParam.builder()
                    .apiKey(API_KEY)
                    .model(MODEL_NAME)
                    .messages(Arrays.asList(systemMsg, userMsg))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();

            // 调用生成API并返回结果
            return gen.call(param);
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            // 捕获异常并抛出，附带错误信息
            throw new Exception("内容生成失败！错误：" + e.getMessage());
        }
    }
}