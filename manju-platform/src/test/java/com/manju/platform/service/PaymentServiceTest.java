package com.manju.platform.service;

import com.manju.platform.dao.UsageLogDao;
import com.manju.platform.dao.UserDao;
import com.manju.platform.entity.UsageLog;
import com.manju.platform.entity.User;
import com.manju.platform.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 积分与支付逻辑测试")
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private UserDao userDao;

    @Mock
    private UsageLogDao logDao;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setPoints(100);
        testUser.setVersion(0);
    }

    // ========== 免费流程测试 ==========

    @Nested
    @DisplayName("PAY-01~03: 免费流程")
    class FreeFlowTests {

        @Test
        @DisplayName("PAY-01: 首次免费 - 插入成功，AI调用成功")
        void firstTimeFree_success() {
            when(logDao.insertAndReturnId(any(UsageLog.class))).thenReturn(1);

            String result = paymentService.processPayment(
                    1, "script_generate", 5,
                    () -> "AI生成的剧本"
            );

            assertEquals("AI生成的剧本", result);
            verify(logDao).insertAndReturnId(argThat(log ->
                    log.getIsFree() == 1 && log.getPointsCost() == 0
            ));
            verify(logDao).updateLogStatus(1, 1, null);
            // 不应查询用户积分（免费流程不走付费）
            verify(userDao, never()).findById(anyInt());
        }

        @Test
        @DisplayName("PAY-02: 首次免费 - AI成功后更新日志状态为1")
        void firstTimeFree_logStatusUpdated() {
            when(logDao.insertAndReturnId(any(UsageLog.class))).thenReturn(42);

            paymentService.processPayment(1, "parse_script", 5, () -> "结果");

            verify(logDao).updateLogStatus(42, 1, null);
        }

        @Test
        @DisplayName("PAY-03: 免费流程 - AI调用失败，事务回滚，不消耗免费次数")
        void firstTimeFree_aiFailure_rollback() {
            when(logDao.insertAndReturnId(any(UsageLog.class))).thenReturn(1);

            assertThrows(RuntimeException.class, () ->
                    paymentService.processPayment(1, "script_generate", 5,
                            () -> { throw new RuntimeException("AI超时"); })
            );

            // 免费日志插入了但事务会回滚（通过@Transactional）
            // updateLogStatus 不应被调用（AI失败了）
            verify(logDao, never()).updateLogStatus(anyInt(), anyInt(), any());
        }
    }

    // ========== 付费流程测试 ==========

    @Nested
    @DisplayName("PAY-04~08: 付费流程")
    class PaidFlowTests {

        @Test
        @DisplayName("PAY-04: 付费正常 - 今日已免费，积分足够，AI成功")
        void paidFlow_success() {
            // 第一次插入抛 DuplicateKey，触发付费流程；第二次正常插入成功
            when(logDao.insertAndReturnId(any(UsageLog.class)))
                    .thenThrow(new DuplicateKeyException("已免费"))
                    .thenReturn(2);
            when(userDao.findById(1)).thenReturn(testUser);
            when(userDao.updatePointsWithVersion(1, 95, 0)).thenReturn(true);

            String result = paymentService.processPayment(
                    1, "script_generate", 5,
                    () -> "付费剧本"
            );

            assertEquals("付费剧本", result);
            // 验证预扣积分：100 - 5 = 95
            verify(userDao).updatePointsWithVersion(1, 95, 0);
        }

        @Test
        @DisplayName("PAY-05: 付费 - 积分不足")
        void paidFlow_insufficientPoints() {
            testUser.setPoints(3); // 不足5积分
            // 第一次抛 DuplicateKey 进入付费流程，第二次是 recordFailLog 调用
            when(logDao.insertAndReturnId(any(UsageLog.class)))
                    .thenThrow(new DuplicateKeyException("已免费"))
                    .thenReturn(99);
            when(userDao.findById(1)).thenReturn(testUser);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    paymentService.processPayment(1, "script_generate", 5, () -> "不会执行")
            );

            assertEquals("积分不足", ex.getMessage());
            verify(userDao, never()).updatePointsWithVersion(anyInt(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("PAY-05b: 付费 - 用户不存在")
        void paidFlow_userNotFound() {
            when(logDao.insertAndReturnId(any(UsageLog.class)))
                    .thenThrow(new DuplicateKeyException("已免费"))
                    .thenReturn(99);
            when(userDao.findById(1)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    paymentService.processPayment(1, "script_generate", 5, () -> "不会执行")
            );

            assertEquals("用户不存在", ex.getMessage());
        }

        @Test
        @DisplayName("PAY-06: 付费 - 乐观锁冲突（version不匹配）")
        void paidFlow_optimisticLockFailure() {
            when(logDao.insertAndReturnId(any(UsageLog.class)))
                    .thenThrow(new DuplicateKeyException("已免费"))
                    .thenReturn(99);
            when(userDao.findById(1)).thenReturn(testUser);
            when(userDao.updatePointsWithVersion(1, 95, 0)).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    paymentService.processPayment(1, "script_generate", 5, () -> "不会执行")
            );

            assertEquals("积分扣费失败，请重试", ex.getMessage());
        }

        @Test
        @DisplayName("PAY-07: 付费 - AI调用失败，积分退还")
        void paidFlow_aiFailure_refund() {
            when(logDao.insertAndReturnId(any(UsageLog.class)))
                    .thenThrow(new DuplicateKeyException("已免费"));
            when(userDao.findById(1)).thenReturn(testUser);
            when(userDao.updatePointsWithVersion(1, 95, 0)).thenReturn(true);
            when(userDao.refundPoints(1, 5)).thenReturn(true);

            assertThrows(RuntimeException.class, () ->
                    paymentService.processPayment(1, "script_generate", 5,
                            () -> { throw new RuntimeException("DeepSeek超时"); })
            );

            // 验证积分退还
            verify(userDao).refundPoints(1, 5);
        }

        @Test
        @DisplayName("PAY-各工具积分定价验证")
        void verifyToolPricing() {
            assertEquals(5, com.manju.platform.common.Constants.POINTS_SCRIPT_GENERATE);
            assertEquals(5, com.manju.platform.common.Constants.POINTS_PARSE_SCRIPT);
            assertEquals(10, com.manju.platform.common.Constants.POINTS_CHARACTER_GENERATE);
            assertEquals(10, com.manju.platform.common.Constants.POINTS_SCENE_GENERATE);
            assertEquals(10, com.manju.platform.common.Constants.POINTS_KEYFRAME_GENERATE);
            assertEquals(20, com.manju.platform.common.Constants.POINTS_VIDEO_GENERATE);
        }

        @Test
        @DisplayName("PAY-视频生成扣20积分")
        void paidFlow_videoGenerate_20points() {
            testUser.setPoints(100);
            when(logDao.insertAndReturnId(any(UsageLog.class)))
                    .thenThrow(new DuplicateKeyException("已免费"))
                    .thenReturn(2);
            when(userDao.findById(1)).thenReturn(testUser);
            when(userDao.updatePointsWithVersion(1, 80, 0)).thenReturn(true);

            paymentService.processPayment(1, "video_generate", 20, () -> "视频结果");

            verify(userDao).updatePointsWithVersion(1, 80, 0);
        }

        @Test
        @DisplayName("PAY-积分刚好够用（边界值）")
        void paidFlow_exactPoints() {
            testUser.setPoints(5);
            when(logDao.insertAndReturnId(any(UsageLog.class)))
                    .thenThrow(new DuplicateKeyException("已免费"))
                    .thenReturn(2);
            when(userDao.findById(1)).thenReturn(testUser);
            when(userDao.updatePointsWithVersion(1, 0, 0)).thenReturn(true);

            String result = paymentService.processPayment(
                    1, "script_generate", 5, () -> "结果"
            );

            assertEquals("结果", result);
            verify(userDao).updatePointsWithVersion(1, 0, 0);
        }
    }
}
