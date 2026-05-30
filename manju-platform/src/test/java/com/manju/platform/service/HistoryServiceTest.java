package com.manju.platform.service;

import com.manju.platform.dao.HistoryDao;
import com.manju.platform.entity.UserHistory;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoryService 历史记录测试")
class HistoryServiceTest {

    @InjectMocks
    private HistoryService historyService;

    @Mock
    private HistoryDao historyDao;

    @Mock
    private HttpSession session;

    @Test
    @DisplayName("H-SAVE: 已登录用户保存历史记录")
    void save_loggedInUser() {
        when(session.getId()).thenReturn("session-123");
        when(historyDao.insert(any())).thenReturn(1);

        UserHistory result = historyService.save(
                1, session, "script_generate",
                "写一个都市...", "AI生成的剧本", null,
                "success", null
        );

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals("script_generate", result.getTool());
        assertEquals("写一个都市...", result.getInputPreview());
        assertEquals("AI生成的剧本", result.getResultText());
        assertEquals("success", result.getStatus());
        assertEquals("session-123", result.getSessionId());
        verify(historyDao).insert(any(UserHistory.class));
    }

    @Test
    @DisplayName("H-SAVE: 未登录用户保存历史记录 (userId=null)")
    void save_guestUser() {
        when(session.getId()).thenReturn("guest-session-456");
        when(historyDao.insert(any())).thenReturn(1);

        UserHistory result = historyService.save(
                null, session, "character_generate",
                "林静，25岁...", null, "https://img.jpg",
                "success", null
        );

        assertNull(result.getUserId());
        assertEquals("guest-session-456", result.getSessionId());
        assertEquals("https://img.jpg", result.getResultUrl());
    }

    @Test
    @DisplayName("H-SAVE: 视频任务保存 (status=pending, taskId不为空)")
    void save_videoTask() {
        when(session.getId()).thenReturn("s1");
        when(historyDao.insert(any())).thenReturn(1);

        UserHistory result = historyService.save(
                1, session, "video_generate",
                "视频描述...", "视频生成中...", null,
                "pending", "task-abc-123"
        );

        assertEquals("pending", result.getStatus());
        assertEquals("task-abc-123", result.getTaskId());
    }

    @Test
    @DisplayName("H-UPDATE: 视频任务完成后更新状态")
    void updateByTaskId_succeeded() {
        UserHistory existing = new UserHistory();
        existing.setId(10);
        existing.setStatus("pending");
        existing.setTaskId("task-abc");

        when(historyDao.findByTaskId("task-abc")).thenReturn(existing);

        historyService.updateByTaskId("task-abc", "success",
                "https://video.mp4", "视频生成成功");

        verify(historyDao).updateById(argThat(h ->
                h.getStatus().equals("success") &&
                        h.getResultUrl().equals("https://video.mp4") &&
                        h.getResultText().equals("视频生成成功")
        ));
    }

    @Test
    @DisplayName("H-UPDATE: 视频任务失败后更新状态")
    void updateByTaskId_failed() {
        UserHistory existing = new UserHistory();
        existing.setId(11);
        existing.setStatus("pending");

        when(historyDao.findByTaskId("task-fail")).thenReturn(existing);

        historyService.updateByTaskId("task-fail", "failed",
                null, "生成失败");

        verify(historyDao).updateById(argThat(h ->
                h.getStatus().equals("failed") && h.getResultUrl() == null
        ));
    }

    @Test
    @DisplayName("H-UPDATE: 已完成的任务不重复更新")
    void updateByTaskId_alreadyCompleted_noUpdate() {
        UserHistory existing = new UserHistory();
        existing.setId(12);
        existing.setStatus("success"); // 已完成

        when(historyDao.findByTaskId("task-done")).thenReturn(existing);

        historyService.updateByTaskId("task-done", "success",
                "https://new.mp4", "新结果");

        // 不应调用 updateById
        verify(historyDao, never()).updateById(any());
    }

    @Test
    @DisplayName("H-UPDATE: taskId不存在时不更新")
    void updateByTaskId_notFound() {
        when(historyDao.findByTaskId("nonexistent")).thenReturn(null);

        historyService.updateByTaskId("nonexistent", "success",
                "url", "text");

        verify(historyDao, never()).updateById(any());
    }

    @Test
    @DisplayName("T-06: 合并游客历史记录到用户")
    void mergeGuestHistory() {
        when(session.getId()).thenReturn("guest-session-789");
        when(historyDao.updateUserIdBySessionId(5, "guest-session-789")).thenReturn(3);

        int merged = historyService.mergeGuestHistoryToUser(session, 5);

        assertEquals(3, merged);
        verify(historyDao).updateUserIdBySessionId(5, "guest-session-789");
    }

    @Test
    @DisplayName("合并游客历史 - 无记录时返回0")
    void mergeGuestHistory_noRecords() {
        when(session.getId()).thenReturn("empty-session");
        when(historyDao.updateUserIdBySessionId(1, "empty-session")).thenReturn(0);

        int merged = historyService.mergeGuestHistoryToUser(session, 1);

        assertEquals(0, merged);
    }
}
