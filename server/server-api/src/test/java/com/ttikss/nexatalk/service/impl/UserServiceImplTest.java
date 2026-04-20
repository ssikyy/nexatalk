package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.dto.AdminUpdateUserRequest;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.security.JwtUtil;
import com.ttikss.nexatalk.service.FollowService;
import com.ttikss.nexatalk.service.LikeService;
import com.ttikss.nexatalk.service.NotificationService;
import com.ttikss.nexatalk.service.PostService;
import com.ttikss.nexatalk.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private FileUtils fileUtils;
    @Mock
    private FollowService followService;
    @Mock
    private PostService postService;
    @Mock
    private LikeService likeService;
    @Mock
    private NotificationService notificationService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        userService = new UserServiceImpl(
                userMapper,
                passwordEncoder,
                jwtUtil,
                redisTemplate,
                fileUtils,
                followService,
                postService,
                likeService,
                notificationService
        );
    }

    @Test
    void adminUpdateUser_rejectsRoleChangeByAdmin() {
        Long operatorId = 1L;
        Long targetUserId = 2L;
        User operator = buildUser(operatorId, User.ROLE_ADMIN);
        User targetUser = buildUser(targetUserId, User.ROLE_USER);
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setRole(User.ROLE_ADMIN);

        when(userMapper.selectById(targetUserId)).thenReturn(targetUser);
        when(userMapper.selectById(operatorId)).thenReturn(operator);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userService.adminUpdateUser(operatorId, targetUserId, request)
        );

        assertEquals(ErrorCode.FORBIDDEN.code(), ex.getCode());
        assertEquals("只有超级管理员才能修改用户角色", ex.getMessage());
        verify(userMapper, never()).update(eq(null), any(LambdaUpdateWrapper.class));
    }

    @Test
    void adminUpdateUser_rejectsSuperAdminAssignmentForEveryone() {
        Long operatorId = 1L;
        Long targetUserId = 2L;
        User operator = buildUser(operatorId, User.ROLE_SUPER_ADMIN);
        User targetUser = buildUser(targetUserId, User.ROLE_USER);
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setRole(User.ROLE_SUPER_ADMIN);

        when(userMapper.selectById(targetUserId)).thenReturn(targetUser);
        when(userMapper.selectById(operatorId)).thenReturn(operator);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userService.adminUpdateUser(operatorId, targetUserId, request)
        );

        assertEquals(ErrorCode.FORBIDDEN.code(), ex.getCode());
        assertEquals("不能将用户设置为超级管理员", ex.getMessage());
        verify(userMapper, never()).update(eq(null), any(LambdaUpdateWrapper.class));
    }

    @Test
    void adminUpdateUser_allowsSuperAdminToToggleAdminAndUser() {
        Long operatorId = 1L;
        Long targetUserId = 2L;
        User operator = buildUser(operatorId, User.ROLE_SUPER_ADMIN);
        User targetUser = buildUser(targetUserId, User.ROLE_USER);
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setRole(User.ROLE_ADMIN);
        request.setNickname("新昵称");

        when(userMapper.selectById(targetUserId)).thenReturn(targetUser);
        when(userMapper.selectById(operatorId)).thenReturn(operator);

        userService.adminUpdateUser(operatorId, targetUserId, request);

        verify(userMapper).update(eq(null), any(LambdaUpdateWrapper.class));
    }

    @Test
    void adminResetPassword_rejectsAdminTargetingAdmin() {
        Long operatorId = 1L;
        Long targetUserId = 2L;
        User operator = buildUser(operatorId, User.ROLE_ADMIN);
        User targetUser = buildUser(targetUserId, User.ROLE_ADMIN);

        when(userMapper.selectById(targetUserId)).thenReturn(targetUser);
        when(userMapper.selectById(operatorId)).thenReturn(operator);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userService.adminResetPassword(operatorId, targetUserId)
        );

        assertEquals(ErrorCode.FORBIDDEN.code(), ex.getCode());
        assertEquals("管理员只能重置普通用户密码", ex.getMessage());
        verify(userMapper, never()).update(eq(null), any(LambdaUpdateWrapper.class));
    }

    @Test
    void adminResetPassword_rejectsAnyTargetSuperAdmin() {
        Long operatorId = 1L;
        Long targetUserId = 2L;
        User operator = buildUser(operatorId, User.ROLE_SUPER_ADMIN);
        User targetUser = buildUser(targetUserId, User.ROLE_SUPER_ADMIN);

        when(userMapper.selectById(targetUserId)).thenReturn(targetUser);
        when(userMapper.selectById(operatorId)).thenReturn(operator);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> userService.adminResetPassword(operatorId, targetUserId)
        );

        assertEquals(ErrorCode.FORBIDDEN.code(), ex.getCode());
        assertEquals("不能重置超级管理员密码", ex.getMessage());
        verify(userMapper, never()).update(eq(null), any(LambdaUpdateWrapper.class));
    }

    @Test
    void adminResetPassword_allowsAdminTargetingNormalUser() {
        Long operatorId = 1L;
        Long targetUserId = 2L;
        User operator = buildUser(operatorId, User.ROLE_ADMIN);
        User targetUser = buildUser(targetUserId, User.ROLE_USER);

        when(userMapper.selectById(targetUserId)).thenReturn(targetUser);
        when(userMapper.selectById(operatorId)).thenReturn(operator);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");

        String password = userService.adminResetPassword(operatorId, targetUserId);

        assertEquals("123456", password);
        verify(userMapper).update(eq(null), any(LambdaUpdateWrapper.class));
        verify(redisTemplate).delete("token:" + targetUserId);
    }

    private User buildUser(Long id, int role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(User.STATUS_NORMAL);
        return user;
    }
}
