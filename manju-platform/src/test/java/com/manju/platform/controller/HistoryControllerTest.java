package com.manju.platform.controller;

import com.manju.platform.common.Result;
import com.manju.platform.dao.HistoryDao;
import com.manju.platform.entity.UserHistory;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoryController 历史记录接口测试")
class HistoryControllerTest {

    @InjectMocks
    private HistoryController historyController;

    @Mock
    private HistoryDao historyDao;

    @Mock
    private HttpSession session;

    @Nested
    @DisplayName("H-REC: 最近记录查询")
    class RecentTests {

        @Test
        @DisplayName("H-REC-01: 已登录 - 查询最近记录")
        void recent_loggedIn_hasRecords() {
            when(session.getAttribute("userId")).thenReturn(1);
            List<UserHistory> records = new ArrayList<>();
            UserHistory h = new UserHistory();
            h.setId(1);
            h.setTool("script_generate");
            h.setCreatedAt(LocalDateTime.now());
            records.add(h);
            when(historyDao.findRecentByUserId(1, 5)).thenReturn(records);

            Result result = historyController.getRecentHistory(5, session);

            assertEquals(200, result.getCode());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("H-REC-02: 已登录 - 无记录")
        void recent_loggedIn_noRecords() {
            when(session.getAttribute("userId")).thenReturn(1);
            when(historyDao.findRecentByUserId(1, 5)).thenReturn(Collections.emptyList());

            Result result = historyController.getRecentHistory(5, session);

            assertEquals(200, result.getCode());
        }

        @Test
        @DisplayName("H-REC-03: 未登录 - 拒绝")
        void recent_notLoggedIn() {
            when(session.getAttribute("userId")).thenReturn(null);

            Result result = historyController.getRecentHistory(5, session);

            assertEquals(500, result.getCode());
            assertEquals("请先登录", result.getMsg());
        }

        @Test
        @DisplayName("H-REC-04: 自定义limit=3")
        void recent_customLimit() {
            when(session.getAttribute("userId")).thenReturn(1);
            when(historyDao.findRecentByUserId(1, 3)).thenReturn(Collections.emptyList());

            historyController.getRecentHistory(3, session);

            verify(historyDao).findRecentByUserId(1, 3);
        }
    }

    @Nested
    @DisplayName("H-LIST: 全部记录分页查询")
    class ListTests {

        @Test
        @DisplayName("H-LIST-01: 分页查询第1页")
        void list_page1() {
            when(session.getAttribute("userId")).thenReturn(1);
            when(historyDao.findByUserId(1, 0, 20)).thenReturn(Collections.emptyList());
            when(historyDao.countByUserId(1)).thenReturn(0);

            Result result = historyController.getHistoryList(1, 20, session);

            assertEquals(200, result.getCode());
            HistoryController.HistoryListResult data = (HistoryController.HistoryListResult) result.getData();
            assertEquals(1, data.page);
            assertEquals(20, data.size);
            assertEquals(0, data.total);
        }

        @Test
        @DisplayName("H-LIST-02: 分页查询第2页 offset=20")
        void list_page2() {
            when(session.getAttribute("userId")).thenReturn(1);
            when(historyDao.findByUserId(1, 20, 20)).thenReturn(Collections.emptyList());
            when(historyDao.countByUserId(1)).thenReturn(30);

            Result result = historyController.getHistoryList(2, 20, session);

            verify(historyDao).findByUserId(1, 20, 20);
        }

        @Test
        @DisplayName("H-LIST-03: 未登录 - 拒绝")
        void list_notLoggedIn() {
            when(session.getAttribute("userId")).thenReturn(null);

            Result result = historyController.getHistoryList(1, 20, session);

            assertEquals(500, result.getCode());
            assertEquals("请先登录", result.getMsg());
        }
    }
}
