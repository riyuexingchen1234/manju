package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.KeyframeGenerateRequest;
import com.manju.platform.dto.KeyframeGenerateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeyframeService 关键帧生成逻辑测试")
class KeyframeServiceTest {

    @InjectMocks
    private KeyframeService keyframeService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private AIService aiService;

    private void mockPaymentPassThrough() {
        when(paymentService.processPayment(anyInt(), any(), anyInt(), any()))
                .thenAnswer(invocation -> {
                    var callable = invocation.getArgument(3, com.manju.platform.functional.AICallable.class);
                    return callable.execute();
                });
    }

    @Test
    @DisplayName("K-GEN-01: 无角色图 - prompt为场景参考图格式")
    void generateKeyframe_noCharacters() {
        mockPaymentPassThrough();
        KeyframeGenerateRequest req = new KeyframeGenerateRequest();
        req.setStoryboardDescription("主角站在窗前凝视远方");
        req.setCharacterImageUrls(Collections.emptyList());
        req.setSceneImageUrl("https://example.com/scene.jpg");

        when(aiService.generateImage(anyString(), anyList()))
                .thenReturn("https://example.com/keyframe.jpg");

        KeyframeGenerateResponse result = keyframeService.generateKeyframe(1, req);

        assertNotNull(result);
        assertEquals("https://example.com/keyframe.jpg", result.getImageUrl());
        verify(aiService).generateImage(argThat(prompt ->
                prompt.contains("场景参考图") && prompt.contains("主角站在窗前凝视远方")
        ), anyList());
    }

    @Test
    @DisplayName("K-GEN-02: 单角色 - prompt为图1角色+图2场景格式")
    void generateKeyframe_singleCharacter() {
        mockPaymentPassThrough();
        KeyframeGenerateRequest req = new KeyframeGenerateRequest();
        req.setStoryboardDescription("林静在办公室工作");
        req.setCharacterImageUrls(List.of("https://example.com/char1.jpg"));
        req.setSceneImageUrl("https://example.com/office.jpg");

        when(aiService.generateImage(anyString(), anyList()))
                .thenReturn("https://example.com/kf.jpg");

        keyframeService.generateKeyframe(1, req);

        verify(aiService).generateImage(
                argThat(prompt ->
                        prompt.contains("图1是角色形象参考图") &&
                                prompt.contains("图2是场景背景参考图") &&
                                prompt.contains("林静在办公室工作")
                ),
                argThat(urls -> urls.size() == 2 &&
                        urls.get(0).equals("https://example.com/char1.jpg") &&
                        urls.get(1).equals("https://example.com/office.jpg"))
        );
    }

    @Test
    @DisplayName("K-GEN-03: 多角色(3个) - prompt为图1到图3角色+图4场景格式")
    void generateKeyframe_multipleCharacters() {
        mockPaymentPassThrough();
        KeyframeGenerateRequest req = new KeyframeGenerateRequest();
        req.setStoryboardDescription("三人对话场景");
        req.setCharacterImageUrls(Arrays.asList(
                "https://example.com/c1.jpg",
                "https://example.com/c2.jpg",
                "https://example.com/c3.jpg"
        ));
        req.setSceneImageUrl("https://example.com/bg.jpg");

        when(aiService.generateImage(anyString(), anyList()))
                .thenReturn("https://example.com/kf3.jpg");

        keyframeService.generateKeyframe(1, req);

        verify(aiService).generateImage(
                argThat(prompt ->
                        prompt.contains("图1到图3是角色形象参考图") &&
                                prompt.contains("图4是场景背景参考图")
                ),
                argThat(urls -> urls.size() == 4)
        );
    }

    @Test
    @DisplayName("K-GEN-04: 无场景图")
    void generateKeyframe_noSceneImage() {
        mockPaymentPassThrough();
        KeyframeGenerateRequest req = new KeyframeGenerateRequest();
        req.setStoryboardDescription("角色独自行走");
        req.setCharacterImageUrls(Collections.emptyList());
        req.setSceneImageUrl(null);

        when(aiService.generateImage(anyString(), anyList()))
                .thenReturn("https://example.com/kf_noscene.jpg");

        KeyframeGenerateResponse result = keyframeService.generateKeyframe(1, req);

        assertNotNull(result);
    }

    @Test
    @DisplayName("K-GEN-05: 积分定价为10")
    void verifyKeyframePricing() {
        assertEquals(10, Constants.POINTS_KEYFRAME_GENERATE);
    }
}
