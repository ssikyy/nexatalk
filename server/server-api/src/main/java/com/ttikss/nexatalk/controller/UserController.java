package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.dto.AdminUpdateUserRequest;
import com.ttikss.nexatalk.dto.UpdatePasswordRequest;
import com.ttikss.nexatalk.dto.UpdateProfileRequest;
import com.ttikss.nexatalk.dto.UserLoginRequest;
import com.ttikss.nexatalk.dto.UserRegisterRequest;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.security.RequireAdmin;
import com.ttikss.nexatalk.service.UserService;
import com.ttikss.nexatalk.vo.LoginVO;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.UserVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户模块控制器
 *
 * 接口清单：
 * POST /api/users/register       注册（公开）
 * POST /api/users/login          登录（公开）
 * POST /api/users/logout         登出（需 Token）
 * GET  /api/users/me             查看自己的信息（需 Token）
 * GET  /api/users/{id}           查看指定用户公开信息（需 Token）
 * PUT  /api/users/me             修改自己的资料（需 Token）
 * PUT  /api/users/me/password    修改密码（需 Token）
 * POST /api/users/me/avatar      上传头像（需 Token）
 *
 * 管理员接口：
 * GET    /api/admin/users        分页查询用户列表（需管理员）
 * PUT    /api/admin/users/{id}   修改指定用户信息（需管理员）
 * POST   /api/admin/users/{id}/reset-password 重置用户密码（需管理员）
 */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 用户注册（公开接口） */
    @PostMapping("/users/register")
    public Result<String> register(@Valid @RequestBody UserRegisterRequest req) {
        userService.register(req);
        return Result.ok("注册成功");
    }

    /** 用户登录（公开接口），返回 JWT Token */
    @PostMapping("/users/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginRequest req) {
        return Result.ok(userService.login(req));
    }

    /** 用户登出，删除 Redis Token */
    @PostMapping("/users/logout")
    public Result<String> logout(@CurrentUser Long userId) {
        userService.logout(userId);
        return Result.ok("已登出");
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/users/me")
    public Result<UserVO> getMe(@CurrentUser Long userId) {
        return Result.ok(userService.getUserById(userId));
    }

    /** 获取当前登录用户的详细信息（含统计信息） */
    @GetMapping("/users/me/detail")
    public Result<UserVO> getMyDetail(@CurrentUser Long userId) {
        return Result.ok(userService.getUserDetailWithStats(userId));
    }

    /** 获取指定用户的公开信息 */
    @GetMapping("/users/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        return Result.ok(userService.getUserById(id));
    }

    /** 获取指定用户的详细信息（含粉丝/关注数） */
    @GetMapping("/users/{id}/detail")
    public Result<UserVO> getUserDetail(@PathVariable Long id) {
        return Result.ok(userService.getUserDetailWithStats(id));
    }

    /** 分页获取用户列表（需登录） */
    @GetMapping("/users")
    public Result<PageVO<UserVO>> listUsers(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(PageVO.from(userService.listUsers(page, pageSize)));
    }

    /** 修改当前用户资料（昵称、简介） */
    @PutMapping("/users/me")
    public Result<String> updateProfile(@CurrentUser Long userId,
                                        @Valid @RequestBody UpdateProfileRequest req) {
        userService.updateProfile(userId, req);
        return Result.ok("修改成功");
    }

    /** 修改密码，修改后强制登出 */
    @PutMapping("/users/me/password")
    public Result<String> updatePassword(@CurrentUser Long userId,
                                         @Valid @RequestBody UpdatePasswordRequest req) {
        userService.updatePassword(userId, req);
        return Result.ok("密码修改成功，请重新登录");
    }

    /** 上传头像 */
    @PostMapping("/users/me/avatar")
    public Result<String> uploadAvatar(@CurrentUser Long userId, MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(userId, file);
        return Result.ok(avatarUrl);
    }

    /** 上传背景图 */
    @PostMapping("/users/me/banner")
    public Result<String> uploadBanner(@CurrentUser Long userId, MultipartFile file) {
        String bannerUrl = userService.uploadBanner(userId, file);
        return Result.ok(bannerUrl);
    }

    // ==================== 管理员接口 ====================

    /** 分页查询用户列表（需管理员） */
    @GetMapping("/admin/users")
    @RequireAdmin
    public Result<PageVO<UserVO>> adminListUsers(@CurrentUser Long currentUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) Integer status) {
        IPage<UserVO> pageResult = userService.adminListUsers(page, pageSize, username, role, status);
        return Result.ok(PageVO.from(pageResult));
    }

    /** 修改指定用户信息（需管理员） */
    @PutMapping("/admin/users/{id}")
    @RequireAdmin
    public Result<String> adminUpdateUser(@CurrentUser Long currentUserId,
                                          @PathVariable Long id,
                                          @Valid @RequestBody AdminUpdateUserRequest req) {
        userService.adminUpdateUser(currentUserId, id, req);
        return Result.ok("修改成功");
    }

    /** 重置用户密码（需管理员） */
    @PostMapping("/admin/users/{id}/reset-password")
    @RequireAdmin
    public Result<String> adminResetPassword(@CurrentUser Long currentUserId, @PathVariable Long id) {
        String newPassword = userService.adminResetPassword(currentUserId, id);
        return Result.ok("密码已重置为: " + newPassword);
    }
}
