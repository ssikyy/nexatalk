package com.ttikss.nexatalk.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ttikss.nexatalk.dto.AdminUpdateUserRequest;
import com.ttikss.nexatalk.dto.UpdatePasswordRequest;
import com.ttikss.nexatalk.dto.UpdateProfileRequest;
import com.ttikss.nexatalk.dto.UserLoginRequest;
import com.ttikss.nexatalk.dto.UserRegisterRequest;
import com.ttikss.nexatalk.vo.LoginVO;
import com.ttikss.nexatalk.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户模块业务层接口
 */
public interface UserService {

    /**
     * 用户注册
     * @param req 注册请求（用户名 + 密码）
     */
    void register(UserRegisterRequest req);

    /**
     * 用户登录
     * 校验密码 → 生成 JWT Token → 写入 Redis
     *
     * @param req 登录请求（用户名 + 密码）
     * @return 登录成功响应（含 Token 和用户信息）
     */
    LoginVO login(UserLoginRequest req);

    /**
     * 用户登出
     * 从 Redis 中删除 Token，使其立即失效
     *
     * @param userId 当前登录用户 ID
     */
    void logout(Long userId);

    /**
     * 查询用户公开信息
     *
     * @param userId 目标用户 ID
     * @return 用户视图对象（不含敏感字段）
     */
    UserVO getUserById(Long userId);

    /**
     * 查询用户详细信息（含统计信息）
     * 包括关注数、粉丝数、帖子数、获赞数
     *
     * @param userId 目标用户 ID
     * @return 用户视图对象（含统计）
     */
    UserVO getUserDetailWithStats(Long userId);

    /**
     * 分页获取用户列表（公开接口）
     *
     * @param page     当前页码
     * @param pageSize 每页数量
     * @return 分页用户列表
     */
    IPage<UserVO> listUsers(int page, int pageSize);

    /**
     * 修改用户资料（昵称、简介）
     *
     * @param userId 当前登录用户 ID
     * @param req    修改请求体
     */
    void updateProfile(Long userId, UpdateProfileRequest req);

    /**
     * 修改密码
     * 需要校验旧密码，修改后使 Token 失效（强制重新登录）
     *
     * @param userId 当前登录用户 ID
     * @param req    修改密码请求体
     */
    void updatePassword(Long userId, UpdatePasswordRequest req);

    /**
     * 上传用户头像
     *
     * @param userId 当前登录用户 ID
     * @param file   头像文件
     * @return 头像访问 URL
     */
    String uploadAvatar(Long userId, MultipartFile file);

    /**
     * 上传用户背景图
     *
     * @param userId 当前登录用户 ID
     * @param file   背景图文件
     * @return 背景图访问 URL
     */
    String uploadBanner(Long userId, MultipartFile file);

    // ==================== 管理员接口 ====================

    /**
     * 管理员分页查询用户列表
     *
     * @param page     当前页码
     * @param pageSize 每页数量
     * @param username 用户名（可选，模糊搜索）
     * @param role     角色筛选（0=普通用户, 1=管理员）
     * @param status   状态筛选（0=正常, 1=禁言, 2=封禁）
     * @return 分页用户列表
     */
    IPage<UserVO> adminListUsers(int page, int pageSize, String username, Integer role, Integer status);

    /**
     * 管理员修改用户信息（可以修改角色和状态）
     *
     * @param operatorId 操作者（当前管理员）ID
     * @param userId 目标用户 ID
     * @param req    修改请求体
     */
    void adminUpdateUser(Long operatorId, Long userId, AdminUpdateUserRequest req);

    /**
     * 管理员重置用户密码为默认密码
     *
     * @param operatorId 操作者（当前管理员）ID
     * @param userId 目标用户 ID
     * @return 默认密码
     */
    String adminResetPassword(Long operatorId, Long userId);
}
