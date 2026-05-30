package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.CharacterGenerateRequest;
import com.manju.platform.dto.CharacterGenerateResponse;
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
@DisplayName("CharacterService 角色生成逻辑测试")
class CharacterServiceTest {

    @InjectMocks
    private CharacterService characterService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private AIService aiService;

    @Test
    @DisplayName("C-GEN-01: 正常生成角色图")
    void generateCharacter_success() {
        CharacterGenerateRequest req = new CharacterGenerateRequest();
        req.setCharacterPrompt("林静，25岁女性，清秀面容");

        when(paymentService.processPayment(eq(1), eq(Constants.TOOL_CHARACTER_GENERATE),
                eq(Constants.POINTS_CHARACTER_GENERATE), any()))
                .thenAnswer(invocation -> {
                    var callable = invocation.getArgument(3, com.manju.platform.functional.AICallable.class);
                    return callable.execute();
                });
        when(aiService.generateImage(anyString(), eq(Collections.emptyList())))
                .thenReturn("https://example.com/char.jpg");

        CharacterGenerateResponse result = characterService.generateCharacter(1, req);

        assertNotNull(result);
        assertEquals("https://example.com/char.jpg", result.getImageUrl());
    }

    @Test
    @DisplayName("C-GEN-02: 带风格声明的角色生成")
    void generateCharacter_withStyle() {
        CharacterGenerateRequest req = new CharacterGenerateRequest();
        req.setCharacterPrompt("林静，25岁");
        req.setStyleDeclaration("古风唯美，暖黄调");

        when(paymentService.processPayment(eq(1), any(), anyInt(), any()))
                .thenAnswer(invocation -> {
                    var callable = invocation.getArgument(3, com.manju.platform.functional.AICallable.class);
                    return callable.execute();
                });
        when(aiService.generateImage(contains("【全局风格声明】古风唯美，暖黄调"), any()))
                .thenReturn("https://example.com/styled.jpg");

        CharacterGenerateResponse result = characterService.generateCharacter(1, req);

        assertEquals("https://example.com/styled.jpg", result.getImageUrl());
        verify(aiService).generateImage(argThat(prompt ->
                prompt.contains("林静，25岁") && prompt.contains("【全局风格声明】古风唯美，暖黄调")
        ), eq(Collections.emptyList()));
    }

    @Test
    @DisplayName("C-GEN-03: 空prompt + 风格声明")
    void generateCharacter_emptyPromptWithStyle() {
        CharacterGenerateRequest req = new CharacterGenerateRequest();
        req.setCharacterPrompt(null);
        req.setStyleDeclaration("现代都市写实风");

        when(paymentService.processPayment(eq(1), any(), anyInt(), any()))
                .thenAnswer(invocation -> {
                    var callable = invocation.getArgument(3, com.manju.platform.functional.AICallable.class);
                    return callable.execute();
                });
        when(aiService.generateImage(anyString(), any()))
                .thenReturn("https://example.com/result.jpg");

        CharacterGenerateResponse result = characterService.generateCharacter(1, req);

        assertNotNull(result);
        verify(aiService).generateImage(argThat(prompt ->
                prompt.contains("【全局风格声明】现代都市写实风")
        ), eq(Collections.emptyList()));
    }

    @Test
    @DisplayName("C-GEN-03b: 空prompt + 空风格声明")
    void generateCharacter_emptyPromptNoStyle() {
        CharacterGenerateRequest req = new CharacterGenerateRequest();
        req.setCharacterPrompt(null);
        req.setStyleDeclaration(null);

        when(paymentService.processPayment(eq(1), any(), anyInt(), any()))
                .thenAnswer(invocation -> {
                    var callable = invocation.getArgument(3, com.manju.platform.functional.AICallable.class);
                    return callable.execute();
                });
        when(aiService.generateImage(eq(""), any()))
                .thenReturn("https://example.com/default.jpg");

        CharacterGenerateResponse result = characterService.generateCharacter(1, req);

        assertNotNull(result);
        verify(aiService).generateImage(eq(""), eq(Collections.emptyList()));
    }
}
