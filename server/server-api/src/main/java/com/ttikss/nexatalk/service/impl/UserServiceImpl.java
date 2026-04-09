package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.dto.AdminUpdateUserRequest;
import com.ttikss.nexatalk.dto.UpdatePasswordRequest;
import com.ttikss.nexatalk.dto.UpdateProfileRequest;
import com.ttikss.nexatalk.dto.UserLoginRequest;
import com.ttikss.nexatalk.dto.UserRegisterRequest;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.security.JwtUtil;
import com.ttikss.nexatalk.service.FollowService;
import com.ttikss.nexatalk.service.LikeService;
import com.ttikss.nexatalk.service.PostService;
import com.ttikss.nexatalk.service.UserService;
import com.ttikss.nexatalk.util.FileUtils;
import com.ttikss.nexatalk.vo.LoginVO;
import com.ttikss.nexatalk.vo.UserVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 用户模块业务实现
 */
@Service
public class UserServiceImpl implements UserService {

    /** Redis 中 Token 的 key 前缀：token:{userId} */
    private static final String TOKEN_KEY_PREFIX = "token:";
    /** Token 在 Redis 中的过期时间（7 天，与 JWT 有效期一致） */
    private static final long TOKEN_EXPIRE_DAYS = 7;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final FileUtils fileUtils;
    private final FollowService followService;
    private final PostService postService;
    private final LikeService likeService;

    public UserServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           StringRedisTemplate redisTemplate,
                           FileUtils fileUtils,
                           FollowService followService,
                           PostService postService,
                           LikeService likeService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.fileUtils = fileUtils;
        this.followService = followService;
        this.postService = postService;
        this.likeService = likeService;
    }

    @Override
    public void register(UserRegisterRequest req) {
        // 1. 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername())
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "用户名已存在");
        }

        // 2. 密码 BCrypt 加密
        String encodedPwd = passwordEncoder.encode(req.getPassword());

        // 3. 写入数据库，默认角色=普通用户，状态=正常
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(encodedPwd);
        // 昵称默认等于用户名
        user.setNickname(req.getUsername());
        user.setRole(User.ROLE_USER);
        user.setStatus(User.STATUS_NORMAL);
        userMapper.insert(user);
    }

    @Override
    public LoginVO login(UserLoginRequest req) {
        // 1. 查找用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername())
        );
        if (user == null) {
            // 故意不区分"用户不存在"和"密码错误"，防止用户名枚举攻击
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "用户名或密码错误");
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "用户名或密码错误");
        }

        // 3. 检查账号状态
        if (Integer.valueOf(User.STATUS_BANNED).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "账号已被封禁，请联系管理员");
        }

        // 4. 生成 JWT Token（包含用户名用于日志追踪）
        String token = jwtUtil.generateToken(user.getId(), user.getRole(), user.getUsername());

        // 5. 将 Token 写入 Redis（支持主动登出：删 key 即失效）
        String redisKey = TOKEN_KEY_PREFIX + user.getId();
        redisTemplate.opsForValue().set(redisKey, token, TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);

        return new LoginVO(
                token,
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getBannerUrl(),
                user.getBio(),
                user.getRole()
        );
    }

    @Override
    public void logout(Long userId) {
        // 删除 Redis 中的 Token，使其立即失效
        redisTemplate.delete(TOKEN_KEY_PREFIX + userId);
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "用户不存在");
        }
        return UserVO.from(user);
    }

    @Override
    public UserVO getUserDetailWithStats(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "用户不存在");
        }
        UserVO vo = UserVO.from(user);

        // 查询统计信息
        long followingCount = followService.countFollowing(userId);
        long followersCount = followService.countFollowers(userId);
        long postCount = postService.countUserPosts(userId);
        long likedCount = likeService.countUserLiked(userId);

        vo.setFollowingCount(followingCount);
        vo.setFollowersCount(followersCount);
        vo.setPostCount(postCount);
        vo.setLikedCount(likedCount);

        return vo;
    }

    @Override
    public IPage<UserVO> listUsers(int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);
        Page<User> pageParam = new Page<>(page, pageSize);
        IPage<User> userPage = userMapper.selectPage(pageParam,
                new LambdaQueryWrapper<User>()
                        .orderByDesc(User::getCreatedAt)
        );
        return userPage.convert(UserVO::from);
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest req) {
        // 只更新非空字段
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId);

        if (StringUtils.hasText(req.getNickname())) {
            wrapper.set(User::getNickname, req.getNickname());
        }
        if (req.getBio() != null) {
            wrapper.set(User::getBio, req.getBio());
        }

        userMapper.update(null, wrapper);
    }

    @Override
    public void updatePassword(Long userId, UpdatePasswordRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "用户不存在");
        }

        // 校验旧密码
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "当前密码错误");
        }

        // 更新为新密码
        String newEncodedPwd = passwordEncoder.encode(req.getNewPassword());
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, newEncodedPwd)
        );

        // 修改密码后，强制登出（删 Redis Token，要求重新登录）
        redisTemplate.delete(TOKEN_KEY_PREFIX + userId);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        // 头像文件最大 2MB
        final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;

        try {
            // 上传到 avatar 子目录
            String avatarUrl = fileUtils.uploadFile(file, "avatar", MAX_AVATAR_SIZE);

            // 更新用户头像 URL
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .set(User::getAvatarUrl, avatarUrl)
            );

            return avatarUrl;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.code(), "头像上传失败");
        }
    }

    @Override
    public String uploadBanner(Long userId, MultipartFile file) {
        // 背景图文件最大 5MB
        final long MAX_BANNER_SIZE = 5 * 1024 * 1024;

        try {
            // 上传到 banner 子目录
            String bannerUrl = fileUtils.uploadFile(file, "banner", MAX_BANNER_SIZE);

            // 更新用户背景图 URL
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .set(User::getBannerUrl, bannerUrl)
            );

            return bannerUrl;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.code(), "背景图上传失败");
        }
    }

    // ==================== 管理员接口实现 ====================

    @Override
    public IPage<UserVO> adminListUsers(int page, int pageSize, String username, Integer role, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username);
        }
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> pageParam = new Page<>(page, pageSize);
        IPage<User> userPage = userMapper.selectPage(pageParam, wrapper);

        return userPage.convert(UserVO::fromWithAdmin);
    }

    @Override
    public void adminUpdateUser(Long operatorId, Long userId, AdminUpdateUserRequest req) {
        User targetUser = userMapper.selectById(userId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "用户不存在");
        }

        // 获取操作者信息，判断是否是超级管理员
        User operator = userMapper.selectById(operatorId);
        boolean isSuperAdmin = operator != null && operator.getRole() != null
                && operator.getRole() == User.ROLE_SUPER_ADMIN;
        // 目标用户是否是超级管理员
        boolean isTargetSuperAdmin = targetUser.getRole() != null
                && targetUser.getRole() == User.ROLE_SUPER_ADMIN;

        // 超级管理员只能由超级管理员操作
        if (isTargetSuperAdmin && !isSuperAdmin) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "不能操作超级管理员");
        }

        // 非超级管理员不能授予超级管理员权限
        if (!isSuperAdmin && req.getRole() != null && req.getRole() == User.ROLE_SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "只有超级管理员才能授予超级管理员权限");
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId);

        if (StringUtils.hasText(req.getNickname())) {
            wrapper.set(User::getNickname, req.getNickname());
        }
        if (req.getBio() != null) {
            wrapper.set(User::getBio, req.getBio());
        }
        if (req.getRole() != null) {
            wrapper.set(User::getRole, req.getRole());
        }
        if (req.getStatus() != null) {
            wrapper.set(User::getStatus, req.getStatus());
            // 如果将用户封禁，需要删除其Redis Token使其立即下线
            if (req.getStatus() == User.STATUS_BANNED) {
                redisTemplate.delete(TOKEN_KEY_PREFIX + userId);
            }
        }

        userMapper.update(null, wrapper);
    }

    @Override
    public String adminResetPassword(Long operatorId, Long userId) {
        User targetUser = userMapper.selectById(userId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "用户不存在");
        }

        // 获取操作者信息，判断是否是超级管理员
        User operator = userMapper.selectById(operatorId);
        boolean isSuperAdmin = operator != null && operator.getRole() != null
                && operator.getRole() == User.ROLE_SUPER_ADMIN;
        // 目标用户是否是超级管理员
        boolean isTargetSuperAdmin = targetUser.getRole() != null
                && targetUser.getRole() == User.ROLE_SUPER_ADMIN;

        // 超级管理员只能由超级管理员操作
        if (isTargetSuperAdmin && !isSuperAdmin) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "不能重置超级管理员密码");
        }

        // 默认密码
        final String DEFAULT_PASSWORD = "123456";
        String encodedPwd = passwordEncoder.encode(DEFAULT_PASSWORD);

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, encodedPwd)
        );

        // 强制登出该用户
        redisTemplate.delete(TOKEN_KEY_PREFIX + userId);

        return DEFAULT_PASSWORD;
    }
}
