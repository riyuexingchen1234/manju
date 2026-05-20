package com.manju.platform.controller;

import com.manju.platform.common.Constants;
import com.manju.platform.common.Result;
import com.manju.platform.dto.VideoGenerateRequest;
import com.manju.platform.dto.VideoGenerateResponse;
import com.manju.platform.service.AIService;
import com.manju.platform.service.GuestTrialService;
import com.manju.platform.service.HistoryService;
import com.manju.platform.service.VideoService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VideoController 视频生成接口测试")
class VideoControllerTest {

    @InjectMocks
    private VideoController videoController;

    @Mock
    private VideoService videoService;

    @Mock
    private AIService aiService;

    @Mock
    private HistoryService historyService;

    @Mock
    private GuestTrialService guestTrialService;

    @Mock
    private HttpSession session;

    @Nested
    @DisplayName("视频任务创建")
    class GenerateTests {

        @Test
        @DisplayName("V-GEN-01: 已登录 - 正常创建视频任务")
        void generate_loggedIn_success() {
            when(session.getAttribute("userId")).thenReturn(1);

            VideoGenerateRequest request = new VideoGenerateRequest();
            request.setKeyframeImageUrl("https://example.com/kf.jpg");
            request.setDescription("林静在办公室");

            VideoGenerateResponse response = new VideoGenerateResponse();
            response.setTaskId("task-123");
            response.setStatus("PENDING");
            when(videoService.generateVideo(1, request)).thenReturn(response);

            Result result = videoController.generateVideo(request, session);

            assertEquals(200, result.getCode());
            assertEquals("视频生成成功", result.getMsg());
            // 验证历史记录写入 pending 状态
            verify(historyService).save(eq(1), eq(session), eq("video_generate"),
                    anyString(), eq("视频生成中..."), isNull(), eq("pending"), eq("task-123"));
        }

        @Test
        @DisplayName("V-GEN: 未登录试用 - 创建视频任务")
        void generate_guest_success() {
            when(session.getAttribute("userId")).thenReturn(null);

            VideoGenerateRequest request = new VideoGenerateRequest();
            request.setKeyframeImageUrl("https://example.com/kf.jpg");
            request.setDescription("测试视频");

            VideoGenerateResponse response = new VideoGenerateResponse();
            response.setTaskId("guest-task-1");
            response.setStatus("PENDING");

            when(guestTrialService.execute(eq(session),
                    eq(Constants.TOOL_VIDEO_GENERATE), eq(Constants.DISPLAY_VIDEO_GENERATE), any()))
                    .thenReturn(response);

            Result result = videoController.generateVideo(request, session);

            assertEquals(200, result.getCode());
            verify(historyService).save(isNull(), eq(session), eq("video_generate"),
                    anyString(), eq("视频生成中..."), isNull(), eq("pending"), eq("guest-task-1"));
        }
    }

    @Nested
    @DisplayName("视频任务查询")
    class QueryTests {

        @Test
        @DisplayName("V-QUERY-02: 任务成功 - 更新历史记录")
        void queryTask_succeeded() {
            Map<String, Object> taskResult = Map.of(
                    "status", "SUCCEEDED",
                    "videoUrl", "https://video.mp4"
            );
            when(aiService.queryVideoTaskResult("task-done")).thenReturn(taskResult);

            Result result = videoController.queryVideoTask("task-done", session);

            assertEquals(200, result.getCode());
            verify(historyService).updateByTaskId("task-done", "success",
                    "https://video.mp4", "视频生成成功");
        }

        @Test
        @DisplayName("V-QUERY-03: 任务失败 - 更新历史记录")
        void queryTask_failed() {
            Map<String, Object> taskResult = Map.of(
                    "status", "FAILED",
                    "error", "生成超时"
            );
            when(aiService.queryVideoTaskResult("task-fail")).thenReturn(taskResult);

            Result result = videoController.queryVideoTask("task-fail", session);

            assertEquals(200, result.getCode());
            verify(historyService).updateByTaskId("task-fail", "failed", null, "生成超时");
        }

        @Test
        @DisplayName("V-QUERY-01: 任务进行中 - 不更新历史记录")
        void queryTask_pending() {
            Map<String, Object> taskResult = Map.of("status", "PENDING");
            when(aiService.queryVideoTaskResult("task-pending")).thenReturn(taskResult);

            Result result = videoController.queryVideoTask("task-pending", session);

            assertEquals(200, result.getCode());
            verify(historyService, never()).updateByTaskId(any(), any(), any(), any());
        }

        @Test
        @DisplayName("V-QUERY: 更新历史记录失败不影响查询结果")
        void queryTask_updateHistoryFails_resultStillReturned() {
            Map<String, Object> taskResult = Map.of(
                    "status", "SUCCEEDED",
                    "videoUrl", "https://video.mp4"
            );
            when(aiService.queryVideoTaskResult("task-x")).thenReturn(taskResult);
            doThrow(new RuntimeException("DB error")).when(historyService)
                    .updateByTaskId(any(), any(), any(), any());

            // 不应抛异常
            Result result = videoController.queryVideoTask("task-x", session);

            assertEquals(200, result.getCode());
        }
    }
}
