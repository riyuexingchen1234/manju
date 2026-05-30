package com.manju.platform.controller;

import com.manju.platform.common.Constants;
import com.manju.platform.common.Result;
import com.manju.platform.dto.ScriptGenerateRequest;
import com.manju.platform.dto.ScriptGenerateResponse;
import com.manju.platform.exception.BusinessException;
import com.manju.platform.service.AIService;
import com.manju.platform.service.GuestTrialService;
import com.manju.platform.service.HistoryService;
import com.manju.platform.service.ScriptService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScriptController 剧本生成/拆解接口测试")
class ScriptControllerTest {

    @InjectMocks
    private ScriptController scriptController;

    @Mock
    private ScriptService scriptService;

    @Mock
    private AIService aiService;

    @Mock
    private HistoryService historyService;

    @Mock
    private GuestTrialService guestTrialService;

    @Mock
    private HttpSession session;

    // ========== 已登录用户测试 ==========

    @Nested
    @DisplayName("已登录用户 - 剧本生成")
    class LoggedInTests {

        @Test
        @DisplayName("S-GEN-01: 已登录 - messages方式正常生成")
        void generate_withMessages_success() {
            when(session.getAttribute("userId")).thenReturn(1);

            ScriptGenerateRequest request = new ScriptGenerateRequest();
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", "写一个都市爽文短剧"));
            request.setMessages(messages);

            ScriptGenerateResponse response = new ScriptGenerateResponse();
            response.setScript("生成的剧本");
            when(scriptService.generateScript(eq(1), any())).thenReturn(response);

            Result result = scriptController.generateScript(request, session);

            assertEquals(200, result.getCode());
            assertEquals("剧本生成成功", result.getMsg());
            verify(historyService).save(eq(1), eq(session), eq("script_generate"),
                    anyString(), eq("生成的剧本"), isNull(), eq("success"), isNull());
        }

        @Test
        @DisplayName("S-GEN-05: 已登录 - prompt方式生成")
        void generate_withPrompt_success() {
            when(session.getAttribute("userId")).thenReturn(1);

            ScriptGenerateRequest request = new ScriptGenerateRequest();
            request.setPrompt("穿越题材");

            ScriptGenerateResponse response = new ScriptGenerateResponse();
            response.setScript("穿越剧本");
            when(scriptService.generateScript(eq(1), anyList())).thenReturn(response);

            Result result = scriptController.generateScript(request, session);

            assertEquals(200, result.getCode());
            // 验证自动构建了 messages
            verify(scriptService).generateScript(eq(1), argThat(msgs ->
                    msgs.size() == 1 &&
                            msgs.get(0).get("role").equals("user") &&
                            msgs.get(0).get("content").equals("穿越题材")
            ));
        }

        @Test
        @DisplayName("S-GEN-06: prompt和messages均为空")
        void generate_noInput() {
            when(session.getAttribute("userId")).thenReturn(1);

            ScriptGenerateRequest request = new ScriptGenerateRequest();

            Result result = scriptController.generateScript(request, session);

            assertEquals(500, result.getCode());
            assertEquals("请求参数错误", result.getMsg());
        }

        @Test
        @DisplayName("S-GEN-09: 历史记录 input_preview 截取前10字")
        void generate_inputPreview_truncated() {
            when(session.getAttribute("userId")).thenReturn(1);

            ScriptGenerateRequest request = new ScriptGenerateRequest();
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", "这是一段非常非常长的用户输入内容，需要截取"));
            request.setMessages(messages);

            ScriptGenerateResponse response = new ScriptGenerateResponse();
            response.setScript("剧本");
            when(scriptService.generateScript(anyInt(), any())).thenReturn(response);

            scriptController.generateScript(request, session);

            verify(historyService).save(eq(1), any(), any(),
                    eq("这是一段非常非常长的..."), any(), any(), any(), any());
        }
    }

    // ========== 未登录用户试用测试 ==========

    @Nested
    @DisplayName("未登录用户 - 剧本生成试用")
    class GuestTests {

        @Test
        @DisplayName("S-GEN-07: 未登录首次试用成功")
        void generate_guest_success() {
            when(session.getAttribute("userId")).thenReturn(null);

            ScriptGenerateRequest request = new ScriptGenerateRequest();
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", "试用"));
            request.setMessages(messages);

            when(guestTrialService.execute(eq(session),
                    eq(Constants.TOOL_SCRIPT_GENERATE), eq(Constants.DISPLAY_SCRIPT_GENERATE), any()))
                    .thenReturn("试用剧本");

            Result result = scriptController.generateScript(request, session);

            assertEquals(200, result.getCode());
            assertEquals("试用成功", result.getMsg());
            // 验证历史记录 userId=null
            verify(historyService).save(isNull(), eq(session), eq("script_generate"),
                    anyString(), eq("试用剧本"), isNull(), eq("success"), isNull());
        }

        @Test
        @DisplayName("S-GEN-08: 未登录第二次试用被拒绝")
        void generate_guest_rejected() {
            when(session.getAttribute("userId")).thenReturn(null);

            ScriptGenerateRequest request = new ScriptGenerateRequest();
            request.setMessages(List.of(Map.of("role", "user", "content", "再试")));

            when(guestTrialService.execute(any(), any(), any(), any()))
                    .thenThrow(new BusinessException("您今日已试用过剧本生成，请登录后使用"));

            assertThrows(BusinessException.class, () ->
                    scriptController.generateScript(request, session)
            );
        }
    }
}
