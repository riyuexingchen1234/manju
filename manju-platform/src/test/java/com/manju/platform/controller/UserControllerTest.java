package com.manju.platform.controller;

import com.manju.platform.dao.UserDao;
import com.manju.platform.entity.User;
import com.manju.platform.service.HistoryService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 用户模块测试")
class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserDao userDao;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private HistoryService historyService;

    @Mock
    private HttpSession session;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$encoded_password");
        testUser.setPoints(100);
        testUser.setVersion(0);
        testUser.setCreateTime(LocalDateTime.now());
    }

    // ========== 登录测试 ==========

    @Nested
    @DisplayName("U-LOGIN: 登录测试")
    class LoginTests {

        @Test
        @DisplayName("U-LOGIN-01: 正常登录")
        void login_success() {
            when(userDao.findByUsername("testuser")).thenReturn(testUser);
            when(passwordEncoder.matches("password123", "$2a$10$encoded_password")).thenReturn(true);
            when(historyService.mergeGuestHistoryToUser(session, 1)).thenReturn(0);

            var result = userController.login("testuser", "password123", session);

            assertEquals(200, result.getCode());
            assertEquals("登录成功", result.getMsg());
            verify(session).setAttribute("userId", 1);
            verify(session).setAttribute("user", testUser);
            verify(session).removeAttribute("trialDateMap");
        }

        @Test
        @DisplayName("U-LOGIN-02: 密码错误")
        void login_wrongPassword() {
            when(userDao.findByUsername("testuser")).thenReturn(testUser);
            when(passwordEncoder.matches("wrongpwd", "$2a$10$encoded_password")).thenReturn(false);

            var result = userController.login("testuser", "wrongpwd", session);

            assertEquals(500, result.getCode());
            assertEquals("用户名或密码错误", result.getMsg());
            verify(session, never()).setAttribute(eq("userId"), any());
        }

        @Test
        @DisplayName("U-LOGIN-03: 用户不存在")
        void login_userNotFound() {
            when(userDao.findByUsername("nonexist")).thenReturn(null);

            var result = userController.login("nonexist", "password", session);

            assertEquals(500, result.getCode());
            assertEquals("用户名或密码错误", result.getMsg());
        }

        @Test
        @DisplayName("U-LOGIN-05: 登录后合并游客历史")
        void login_mergeGuestHistory() {
            when(userDao.findByUsername("testuser")).thenReturn(testUser);
            when(passwordEncoder.matches("pwd", "$2a$10$encoded_password")).thenReturn(true);
            when(historyService.mergeGuestHistoryToUser(session, 1)).thenReturn(3);

            userController.login("testuser", "pwd", session);

            verify(historyService).mergeGuestHistoryToUser(session, 1);
        }
    }

    // ========== 注册测试 ==========

    @Nested
    @DisplayName("U-REG: 注册测试")
    class RegisterTests {

        @Test
        @DisplayName("U-REG-01: 正常注册")
        void register_success() {
            when(userDao.findByUsername("newuser")).thenReturn(null);
            when(passwordEncoder.encode("123456")).thenReturn("$2a$10$encoded");
            when(userDao.insert(any(User.class))).thenReturn(1);

            var result = userController.register(
                    Map.of("username", "newuser", "password", "123456")
            );

            assertEquals(200, result.getCode());
            assertEquals("注册成功", result.getMsg());
            verify(userDao).insert(argThat(user ->
                    user.getUsername().equals("newuser") &&
                            user.getPoints() == 100 &&
                            user.getVersion() == 0
            ));
        }

        @Test
        @DisplayName("U-REG-02: 用户名已存在")
        void register_usernameExists() {
            when(userDao.findByUsername("testuser")).thenReturn(testUser);

            var result = userController.register(
                    Map.of("username", "testuser", "password", "123456")
            );

            assertEquals(500, result.getCode());
            assertEquals("用户名已存在", result.getMsg());
        }

        @Test
        @DisplayName("U-REG-03: 用户名为空")
        void register_emptyUsername() {
            var result = userController.register(
                    Map.of("username", "", "password", "123456")
            );

            assertEquals(500, result.getCode());
            assertTrue(result.getMsg().contains("用户名不能为空"));
        }

        @Test
        @DisplayName("U-REG-04: 密码过短")
        void register_shortPassword() {
            var result = userController.register(
                    Map.of("username", "newuser", "password", "123")
            );

            assertEquals(500, result.getCode());
            assertTrue(result.getMsg().contains("密码至少6位"));
        }

        @Test
        @DisplayName("U-REG-07: 密码刚好6位")
        void register_exactSixChars() {
            when(userDao.findByUsername("user6")).thenReturn(null);
            when(passwordEncoder.encode("123456")).thenReturn("$2a$10$encoded");
            when(userDao.insert(any(User.class))).thenReturn(1);

            var result = userController.register(
                    Map.of("username", "user6", "password", "123456")
            );

            assertEquals(200, result.getCode());
        }

        @Test
        @DisplayName("U-REG-初始积分100")
        void register_initialPoints() {
            when(userDao.findByUsername("newuser")).thenReturn(null);
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(userDao.insert(any())).thenReturn(1);

            userController.register(Map.of("username", "newuser", "password", "123456"));

            verify(userDao).insert(argThat(user -> user.getPoints() == 100));
        }
    }

    // ========== 登出测试 ==========

    @Nested
    @DisplayName("U-LOGOUT: 登出测试")
    class LogoutTests {

        @Test
        @DisplayName("U-LOGOUT-01: 正常登出")
        void logout_success() {
            var result = userController.logout(session);

            assertEquals(200, result.getCode());
            verify(session).invalidate();
        }
    }

    // ========== 积分查询测试 ==========

    @Nested
    @DisplayName("U-PTS: 积分查询测试")
    class PointsTests {

        @Test
        @DisplayName("U-PTS-01: 已登录查询积分")
        void getPoints_loggedIn() {
            when(session.getAttribute("userId")).thenReturn(1);
            when(userDao.findById(1)).thenReturn(testUser);

            var result = userController.getPoints(session);

            assertEquals(200, result.getCode());
            assertEquals(100, result.getData());
        }

        @Test
        @DisplayName("U-PTS-02: 未登录查询积分")
        void getPoints_notLoggedIn() {
            when(session.getAttribute("userId")).thenReturn(null);

            var result = userController.getPoints(session);

            assertEquals(500, result.getCode());
            assertEquals("请先登录", result.getMsg());
        }

        @Test
        @DisplayName("U-PTS: 用户不存在")
        void getPoints_userNotFound() {
            when(session.getAttribute("userId")).thenReturn(999);
            when(userDao.findById(999)).thenReturn(null);

            var result = userController.getPoints(session);

            assertEquals(500, result.getCode());
            assertEquals("用户不存在", result.getMsg());
        }
    }
}
