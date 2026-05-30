package com.manju.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIService AI调用容错测试")
class AIServiceTest {

    @InjectMocks
    private AIService aiService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiService, "deepseekApiUrl", "https://api.deepseek.com/chat/completions");
        ReflectionTestUtils.setField(aiService, "deepseekApiKey", "test-key");
        ReflectionTestUtils.setField(aiService, "aliyunImageApiUrl", "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation");
        ReflectionTestUtils.setField(aiService, "aliyunApiKey", "test-aliyun-key");
        ReflectionTestUtils.setField(aiService, "videoApiUrl", "https://dashscope.aliyuncs.com/api/v1/services/aigc/video-generation/video-synthesis");
        ReflectionTestUtils.setField(aiService, "videoApiKey", "test-video-key");
        ReflectionTestUtils.setField(aiService, "videoResultUrl", "https://dashscope.aliyuncs.com/api/v1/tasks/{task_id}");
    }

    // ========== 测试模式测试 ==========

    @Nested
    @DisplayName("测试模式")
    class TestModeTests {

        @Test
        @DisplayName("测试模式 - 剧本生成返回模拟数据")
        void testMode_generateScript() {
            AIService.enableTestMode();
            try {
                List<Map<String, String>> messages = List.of(
                        Map.of("role", "user", "content", "写剧本")
                );
                String result = aiService.generateScript(messages);
                assertEquals("模拟剧本生成测试", result);
            } finally {
                AIService.disableTestMode();
            }
        }

        @Test
        @DisplayName("测试模式 - 超时模拟")
        void testMode_timeout() {
            AIService.enableTestMode();
            try {
                List<Map<String, String>> messages = List.of(
                        Map.of("role", "user", "content", "#timeout")
                );
                assertThrows(RuntimeException.class, () ->
                        aiService.generateScript(messages)
                );
            } finally {
                AIService.disableTestMode();
            }
        }

        @Test
        @DisplayName("测试模式 - 拆解剧本返回有效JSON")
        void testMode_parseScript() {
            AIService.enableTestMode();
            try {
                String result = aiService.parseScript("测试剧本");
                assertNotNull(result);
                assertTrue(result.contains("characters"));
                assertTrue(result.contains("storyboards"));
            } finally {
                AIService.disableTestMode();
            }
        }

        @Test
        @DisplayName("测试模式 - 图片生成返回模拟URL")
        void testMode_generateImage() {
            AIService.enableTestMode();
            try {
                String url = aiService.generateImage("测试prompt", Collections.emptyList());
                assertEquals("https://example.com/test-image.jpg", url);
            } finally {
                AIService.disableTestMode();
            }
        }

        @Test
        @DisplayName("测试模式 - 视频任务返回模拟数据")
        void testMode_createVideoTask() {
            AIService.enableTestMode();
            try {
                Map<String, String> result = aiService.createVideoGenerationTask(
                        "https://img.jpg", "描述");
                assertEquals("test-task-id", result.get("taskId"));
                assertEquals("SUCCEEDED", result.get("status"));
            } finally {
                AIService.disableTestMode();
            }
        }
    }

    // ========== AI 调用失败容错测试 ==========

    @Nested
    @DisplayName("EX-AI: AI调用失败容错")
    class AIFailureTests {

        @Test
        @DisplayName("EX-AI-01: DeepSeek API不可达")
        void deepseek_unreachable() {
            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", "测试")
            );

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    aiService.generateScript(messages)
            );
            assertTrue(ex.getMessage().contains("调用DeepSeek生成剧本失败"));
        }

        @Test
        @DisplayName("EX-AI-03: DeepSeek响应超时")
        void deepseek_timeout() {
            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Read timed out"));

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    aiService.generateScript(List.of(Map.of("role", "user", "content", "x")))
            );
            assertTrue(ex.getMessage().contains("调用DeepSeek生成剧本失败"));
        }

        @Test
        @DisplayName("EX-AI: 拆解API不可达")
        void parseScript_apiFailure() {
            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Network error"));

            assertThrows(RuntimeException.class, () ->
                    aiService.parseScript("测试剧本")
            );
        }

        @Test
        @DisplayName("EX-AI: 图片生成API失败")
        void generateImage_apiFailure() {
            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Service unavailable"));

            assertThrows(RuntimeException.class, () ->
                    aiService.generateImage("测试prompt", Collections.emptyList())
            );
        }

        @Test
        @DisplayName("EX-AI-06: 视频任务创建失败")
        void createVideoTask_apiFailure() {
            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Connection failed"));

            assertThrows(RuntimeException.class, () ->
                    aiService.createVideoGenerationTask("https://img.jpg", "描述")
            );
        }

        @Test
        @DisplayName("EX-AI: 查询视频任务失败")
        void queryVideoTask_apiFailure() {
            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Timeout"));

            assertThrows(RuntimeException.class, () ->
                    aiService.queryVideoTaskResult("task-123")
            );
        }
    }

    // ========== 正常响应解析测试 ==========

    @Nested
    @DisplayName("正常响应解析")
    class ResponseParsingTests {

        @Test
        @DisplayName("剧本生成 - 正常解析DeepSeek响应")
        void generateScript_parseResponse() throws Exception {
            String responseBody = """
                    {"choices":[{"message":{"content":"生成的剧本内容"}}]}
                    """;

            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

            // 使用真实的ObjectMapper
            ObjectMapper realMapper = new ObjectMapper();
            ReflectionTestUtils.setField(aiService, "objectMapper", realMapper);

            String result = aiService.generateScript(List.of(
                    Map.of("role", "user", "content", "测试")
            ));

            assertEquals("生成的剧本内容", result);
        }

        @Test
        @DisplayName("拆解剧本 - 清理markdown包裹")
        void parseScript_cleanMarkdown() throws Exception {
            String responseBody = """
                    {"choices":[{"message":{"content":"```json\\n{\\"characters\\":[], \\"storyboards\\":[]}\\n```"}}]}
                    """;

            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

            ObjectMapper realMapper = new ObjectMapper();
            ReflectionTestUtils.setField(aiService, "objectMapper", realMapper);

            String result = aiService.parseScript("测试剧本");

            assertTrue(result.contains("characters"));
            assertFalse(result.contains("```"));
        }
    }
}
