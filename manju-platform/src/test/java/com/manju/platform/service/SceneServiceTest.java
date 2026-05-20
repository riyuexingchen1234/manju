package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.SceneGenerateRequest;
import com.manju.platform.dto.SceneGenerateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SceneService 场景生成逻辑测试")
class SceneServiceTest {

    @InjectMocks
    private SceneService sceneService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private AIService aiService;

    @Test
    @DisplayName("SC-GEN-01: 正常生成场景图")
    void generateScene_success() {
        SceneGenerateRequest req = new SceneGenerateRequest();
        req.setScenePrompt("现代都市办公室，落地窗");

        when(paymentService.processPayment(eq(1), eq(Constants.TOOL_SCENE_GENERATE),
                eq(Constants.POINTS_SCENE_GENERATE), any()))
                .thenAnswer(invocation -> {
                    var callable = invocation.getArgument(3, com.manju.platform.functional.AICallable.class);
                    return callable.execute();
                });
        when(aiService.generateImage(anyString(), eq(Collections.emptyList())))
                .thenReturn("https://example.com/scene.jpg");

        SceneGenerateResponse result = sceneService.generateScene(1, req);

        assertNotNull(result);
        assertEquals("https://example.com/scene.jpg", result.getImageUrl());
    }

    @Test
    @DisplayName("SC-GEN-02: 带风格声明的场景生成")
    void generateScene_withStyle() {
        SceneGenerateRequest req = new SceneGenerateRequest();
        req.setScenePrompt("古风庭院");
        req.setStyleDeclaration("水墨风格");

        when(paymentService.processPayment(eq(1), any(), anyInt(), any()))
                .thenAnswer(invocation -> {
                    var callable = invocation.getArgument(3, com.manju.platform.functional.AICallable.class);
                    return callable.execute();
                });
        when(aiService.generateImage(anyString(), any()))
                .thenReturn("https://example.com/styled-scene.jpg");

        SceneGenerateResponse result = sceneService.generateScene(1, req);

        verify(aiService).generateImage(argThat(prompt ->
                prompt.contains("古风庭院") && prompt.contains("【全局风格声明】水墨风格")
        ), eq(Collections.emptyList()));
    }

    @Test
    @DisplayName("SC-GEN-03: 积分定价为10")
    void verifyScenePricing() {
        assertEquals(10, Constants.POINTS_SCENE_GENERATE);
    }
}
