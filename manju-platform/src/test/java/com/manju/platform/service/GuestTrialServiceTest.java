package com.manju.platform.service;

import com.manju.platform.exception.BusinessException;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuestTrialService 未登录试用机制测试")
class GuestTrialServiceTest {

    @InjectMocks
    private GuestTrialService guestTrialService;

    @Mock
    private TrialManager trialManager;

    @Mock
    private HttpSession session;

    @Test
    @DisplayName("T-01: 首次试用成功")
    void firstTrial_success() {
        when(trialManager.canTrial(session, "script_generate")).thenReturn(true);

        String result = guestTrialService.execute(
                session, "script_generate", "剧本生成",
                () -> "AI生成结果"
        );

        assertEquals("AI生成结果", result);
        verify(trialManager).recordTrial(session, "script_generate");
    }

    @Test
    @DisplayName("T-02: 当日第二次试用被拒绝")
    void secondTrial_rejected() {
        when(trialManager.canTrial(session, "script_generate")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                guestTrialService.execute(
                        session, "script_generate", "剧本生成",
                        () -> "不应执行"
                )
        );

        assertEquals("您今日已试用过剧本生成，请登录后使用", ex.getMessage());
        verify(trialManager, never()).recordTrial(any(), any());
    }

    @Test
    @DisplayName("T-03: 不同工具独立限制")
    void differentTools_independent() {
        when(trialManager.canTrial(session, "script_generate")).thenReturn(false);
        when(trialManager.canTrial(session, "character_generate")).thenReturn(true);

        // 剧本生成已试用
        assertThrows(BusinessException.class, () ->
                guestTrialService.execute(session, "script_generate", "剧本生成", () -> "x")
        );

        // 角色生成仍可试用
        String result = guestTrialService.execute(
                session, "character_generate", "角色生成",
                () -> "角色图URL"
        );
        assertEquals("角色图URL", result);
    }

    @Test
    @DisplayName("T-05: AI调用失败不记录试用")
    void aiFailure_noRecord() {
        when(trialManager.canTrial(session, "script_generate")).thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                guestTrialService.execute(
                        session, "script_generate", "剧本生成",
                        () -> { throw new RuntimeException("AI超时"); }
                )
        );

        // AI失败后不应记录试用
        verify(trialManager, never()).recordTrial(any(), any());
    }

    @Test
    @DisplayName("T-06: 所有6种工具试用场景")
    void allSixTools_trial() {
        String[] tools = {"script_generate", "parse_script", "character_generate",
                "scene_generate", "keyframe_generate", "video_generate"};
        String[] displays = {"剧本生成", "拆解剧本", "角色生成",
                "场景生成", "关键帧生成", "视频生成"};

        for (int i = 0; i < tools.length; i++) {
            when(trialManager.canTrial(session, tools[i])).thenReturn(true);
            String result = guestTrialService.execute(
                    session, tools[i], displays[i],
                    () -> "result_" + "ok"
            );
            assertEquals("result_ok", result);
            verify(trialManager).recordTrial(session, tools[i]);
        }
    }
}
