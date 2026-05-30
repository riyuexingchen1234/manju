package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.ScriptGenerateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScriptService 剧本生成逻辑测试")
class ScriptServiceTest {

    @InjectMocks
    private ScriptService scriptService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private AIService aiService;

    @Test
    @DisplayName("S-GEN: 正常生成剧本 - 调用PaymentService和AIService")
    void generateScript_success() {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", "写一个都市爽文短剧")
        );

        ScriptGenerateResponse expectedResp = new ScriptGenerateResponse();
        expectedResp.setScript("AI生成的剧本内容");

        when(paymentService.processPayment(eq(1), eq(Constants.TOOL_SCRIPT_GENERATE),
                eq(Constants.POINTS_SCRIPT_GENERATE), any()))
                .thenAnswer(invocation -> {
                    // 模拟PaymentService执行callable
                    var callable = invocation.getArgument(3, com.manju.platform.functional.AICallable.class);
                    return callable.execute();
                });
        when(aiService.generateScript(messages)).thenReturn("AI生成的剧本内容");

        ScriptGenerateResponse result = scriptService.generateScript(1, messages);

        assertNotNull(result);
        assertEquals("AI生成的剧本内容", result.getScript());
        verify(paymentService).processPayment(eq(1), eq("script_generate"), eq(5), any());
    }

    @Test
    @DisplayName("S-GEN: 积分不足时PaymentService抛异常")
    void generateScript_insufficientPoints() {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", "写剧本")
        );

        when(paymentService.processPayment(eq(1), any(), anyInt(), any()))
                .thenThrow(new com.manju.platform.exception.BusinessException("积分不足"));

        assertThrows(com.manju.platform.exception.BusinessException.class, () ->
                scriptService.generateScript(1, messages)
        );
    }
}
