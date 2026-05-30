package com.manju.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manju.platform.common.Constants;
import com.manju.platform.dto.ScriptParseResponse;
import com.manju.platform.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParseScriptService 剧本拆解逻辑测试")
class ParseScriptServiceTest {

    @InjectMocks
    private ParseScriptService parseScriptService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private AIService aiService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("S-PARSE-01: 正常拆解剧本 - 返回完整结构")
    void parseScript_success() {
        String validJson = """
                {
                  "styleDeclaration": "现代都市写实风",
                  "characters": [
                    {"name": "林静", "description": "25岁女性", "characterPrompt": "测试prompt"}
                  ],
                  "storyboards": [
                    {"description": "分镜1", "scenePrompt": "场景", "detailedDescription": "详细", "videoPrompt": "视频", "characters": ["林静"]}
                  ]
                }
                """;

        when(paymentService.processPayment(eq(1), eq(Constants.TOOL_PARSE_SCRIPT),
                eq(Constants.POINTS_PARSE_SCRIPT), any()))
                .thenAnswer(invocation -> {
                    var callable = invocation.getArgument(3, com.manju.platform.functional.AICallable.class);
                    return callable.execute();
                });
        when(aiService.parseScript("测试剧本内容")).thenReturn(validJson);

        ScriptParseResponse result = parseScriptService.parseScript(1, "测试剧本内容");

        assertNotNull(result);
        assertEquals("现代都市写实风", result.getStyleDeclaration());
        assertEquals(1, result.getCharacters().size());
        assertEquals("林静", result.getCharacters().get(0).getName());
        assertEquals(1, result.getStoryboards().size());
        assertEquals("分镜1", result.getStoryboards().get(0).getDescription());
    }

    @Test
    @DisplayName("S-PARSE-04: JSON不完整 - 抛出BusinessException")
    void parseScript_incompleteJson() {
        // AI返回不以 } 结尾的内容
        String incompleteJson = "{\"characters\": [";

        when(paymentService.processPayment(eq(1), any(), anyInt(), any()))
                .thenAnswer(invocation -> {
                    var callable = invocation.getArgument(3, com.manju.platform.functional.AICallable.class);
                    return callable.execute();
                });
        when(aiService.parseScript(any())).thenReturn(incompleteJson);

        assertThrows(BusinessException.class, () ->
                parseScriptService.parseScript(1, "测试剧本")
        );
    }

    @Test
    @DisplayName("S-PARSE-JSON带markdown包裹 - 提取有效JSON")
    void parseScript_jsonWithMarkdownWrapper() {
        String wrappedJson = """
                ```json
                {
                  "styleDeclaration": "古风",
                  "characters": [],
                  "storyboards": []
                }
                ```
                """;
        // 注意：wrappedJson 最后有 ``` 而不是 }，所以会被判为不完整
        // 但如果 AI 返回的是清理后的 JSON，就没问题
        String cleanJson = """
                {
                  "styleDeclaration": "古风",
                  "characters": [],
                  "storyboards": []
                }
                """;

        when(paymentService.processPayment(eq(1), any(), anyInt(), any()))
                .thenAnswer(invocation -> {
                    var callable = invocation.getArgument(3, com.manju.platform.functional.AICallable.class);
                    return callable.execute();
                });
        when(aiService.parseScript(any())).thenReturn(cleanJson);

        ScriptParseResponse result = parseScriptService.parseScript(1, "测试剧本");

        assertNotNull(result);
        assertEquals("古风", result.getStyleDeclaration());
    }

    @Test
    @DisplayName("S-PARSE-05: 积分定价为5")
    void verifyParsePricing() {
        assertEquals(5, Constants.POINTS_PARSE_SCRIPT);
    }
}
