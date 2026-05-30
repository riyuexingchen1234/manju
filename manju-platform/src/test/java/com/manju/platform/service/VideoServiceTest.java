package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.VideoGenerateRequest;
import com.manju.platform.dto.VideoGenerateResponse;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("VideoService 视频生成逻辑测试")
class VideoServiceTest {

    @InjectMocks
    private VideoService videoService;

    @Mock
    private AIService aiService;

    @Mock
    private PaymentService paymentService;

    @Test
    @DisplayName("V-GEN-01: 正常创建视频任务")
    void generateVideo_success() {
        VideoGenerateRequest req = new VideoGenerateRequest();
        req.setKeyframeImageUrl("https://example.com/keyframe.jpg");
        req.setDescription("林静在办公室工作的场景");

        when(paymentService.processPayment(eq(1), eq(Constants.TOOL_VIDEO_GENERATE),
                eq(Constants.POINTS_VIDEO_GENERATE), any()))
                .thenAnswer(invocation -> {
                    var callable = invocation.getArgument(3, com.manju.platform.functional.AICallable.class);
                    return callable.execute();
                });
        when(aiService.createVideoGenerationTask(
                "https://example.com/keyframe.jpg",
                "林静在办公室工作的场景"))
                .thenReturn(Map.of("taskId", "task-123", "status", "PENDING"));

        VideoGenerateResponse result = videoService.generateVideo(1, req);

        assertNotNull(result);
        assertEquals("task-123", result.getTaskId());
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    @DisplayName("V-GEN-02: 视频生成扣20积分")
    void generateVideo_pricing() {
        assertEquals(20, Constants.POINTS_VIDEO_GENERATE);

        VideoGenerateRequest req = new VideoGenerateRequest();
        req.setKeyframeImageUrl("https://example.com/kf.jpg");
        req.setDescription("测试");

        when(paymentService.processPayment(eq(1), eq("video_generate"), eq(20), any()))
                .thenAnswer(invocation -> {
                    var callable = invocation.getArgument(3, com.manju.platform.functional.AICallable.class);
                    return callable.execute();
                });
        when(aiService.createVideoGenerationTask(any(), any()))
                .thenReturn(Map.of("taskId", "t1", "status", "PENDING"));

        videoService.generateVideo(1, req);

        verify(paymentService).processPayment(eq(1), eq("video_generate"), eq(20), any());
    }

    @Test
    @DisplayName("V-GEN-AI失败: 创建任务失败时积分退还(由PaymentService处理)")
    void generateVideo_aiFailure() {
        VideoGenerateRequest req = new VideoGenerateRequest();
        req.setKeyframeImageUrl("https://example.com/kf.jpg");
        req.setDescription("测试");

        when(paymentService.processPayment(eq(1), any(), anyInt(), any()))
                .thenThrow(new RuntimeException("创建视频生成任务失败"));

        assertThrows(RuntimeException.class, () ->
                videoService.generateVideo(1, req)
        );
    }
}
