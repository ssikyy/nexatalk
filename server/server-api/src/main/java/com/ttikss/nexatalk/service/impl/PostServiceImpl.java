package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.dto.PostCreateRequest;
import com.ttikss.nexatalk.entity.Post;
import com.ttikss.nexatalk.entity.Section;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.BlacklistMapper;
import com.ttikss.nexatalk.mapper.FollowMapper;
import com.ttikss.nexatalk.mapper.PostLikeMapper;
import com.ttikss.nexatalk.mapper.PostMapper;
import com.ttikss.nexatalk.mapper.SectionMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.mapper.FavoriteMapper;
import com.ttikss.nexatalk.security.UserContext;
import com.ttikss.nexatalk.service.AiService;
import com.ttikss.nexatalk.service.PostService;
import com.ttikss.nexatalk.util.FileUtils;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PostVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 帖子模块业务实现
 */
@Service
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final SectionMapper sectionMapper;
    private final FileUtils fileUtils;
    private final AiService aiService;
    private final BlacklistMapper blacklistMapper;
    private final FollowMapper followMapper;
    private final PostLikeMapper postLikeMapper;
    private final FavoriteMapper favoriteMapper;

    public PostServiceImpl(PostMapper postMapper,
                           UserMapper userMapper,
                           SectionMapper sectionMapper,
                           FileUtils fileUtils,
                           AiService aiService,
                           BlacklistMapper blacklistMapper,
                           FollowMapper followMapper,
                           PostLikeMapper postLikeMapper,
                           FavoriteMapper favoriteMapper) {
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.sectionMapper = sectionMapper;
        this.fileUtils = fileUtils;
        this.aiService = aiService;
        this.blacklistMapper = blacklistMapper;
        this.followMapper = followMapper;
        this.postLikeMapper = postLikeMapper;
        this.favoriteMapper = favoriteMapper;
    }

    @Override
    public Long createPost(Long userId, PostCreateRequest req) {
        // 检查用户账号状态（禁言用户不能发帖）
        checkUserNotMuted(userId);

        // 内容长度校验
        if (req.getContent() != null && req.getContent().length() > Post.MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "内容长度不能超过 " + Post.MAX_CONTENT_LENGTH + " 个字符");
        }
        if (req.getTitle() != null && req.getTitle().length() > Post.MAX_TITLE_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "标题长度不能超过 " + Post.MAX_TITLE_LENGTH + " 个字符");
        }

        // 检查分区是否存在且正常
        Section section = sectionMapper.selectById(req.getSectionId());
        if (section == null || Integer.valueOf(Section.STATUS_DISABLED).equals(section.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "分区不存在或已禁用");
        }

        Post post = new Post();
        post.setUserId(userId);
        post.setSectionId(req.getSectionId());
        post.setTitle(req.getTitle());

        // 如果请求使用 AI 润色，则调用 AI 服务
        String content = req.getContent();
        if (Boolean.TRUE.equals(req.getAiPolish())) {
            content = aiService.polish(content);
        }
        post.setContent(content);

        post.setCoverUrl(StringUtils.hasText(req.getCoverUrl()) ? req.getCoverUrl() : null);
        // 保存图片列表为JSON
        post.setImages(serializeImages(req.getImages()));
        // 保存标签列表为JSON
        post.setTags(serializeTags(req.getTags()));
        // draft=true 保存草稿，否则直接发布（状态=正常）
        post.setStatus(Boolean.TRUE.equals(req.getDraft()) ? Post.STATUS_DRAFT : Post.STATUS_NORMAL);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setFavoriteCount(0);
        post.setShareCount(0);

        postMapper.insert(post);
        return post.getId();
    }

    @Override
    public void updatePost(Long userId, Long postId, PostCreateRequest req) {
        Post post = getPostOrThrow(postId);
        checkOwnerOrAdmin(userId, post.getUserId());

        // 已删除的帖子不能编辑
        if (Integer.valueOf(Post.STATUS_DELETED).equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "帖子已删除，无法编辑");
        }

        // 如果改了分区，检查新分区是否正常
        if (req.getSectionId() != null && !req.getSectionId().equals(post.getSectionId())) {
            Section section = sectionMapper.selectById(req.getSectionId());
            if (section == null || Integer.valueOf(Section.STATUS_DISABLED).equals(section.getStatus())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "目标分区不存在或已禁用");
            }
        }

        LambdaUpdateWrapper<Post> wrapper = new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId);

        if (req.getSectionId() != null) wrapper.set(Post::getSectionId, req.getSectionId());
        if (StringUtils.hasText(req.getTitle())) wrapper.set(Post::getTitle, req.getTitle());

        // 如果请求使用 AI 润色，则调用 AI 服务
        if (StringUtils.hasText(req.getContent())) {
            String content = req.getContent();
            if (Boolean.TRUE.equals(req.getAiPolish())) {
                content = aiService.polish(content);
            }
            wrapper.set(Post::getContent, content);
        }

        if (req.getCoverUrl() != null) wrapper.set(Post::getCoverUrl, req.getCoverUrl());
        if (req.getImages() != null) wrapper.set(Post::getImages, serializeImages(req.getImages()));
        // 更新标签
        if (req.getTags() != null) {
            wrapper.set(Post::getTags, serializeTags(req.getTags()));
        }

        // 草稿可以更新发布状态（发布时需检查禁言状态）
        if (req.getDraft() != null && !Boolean.TRUE.equals(req.getDraft())) {
            // 从草稿发布，需要检查禁言状态
            checkUserNotMuted(userId);
            wrapper.set(Post::getStatus, Post.STATUS_NORMAL);
        } else if (req.getDraft() != null) {
            wrapper.set(Post::getStatus, Post.STATUS_DRAFT);
        }

        postMapper.update(null, wrapper);
    }

    @Override
    public void deletePost(Long userId, Long postId) {
        Post post = getPostOrThrow(postId);
        checkOwnerOrAdmin(userId, post.getUserId());

        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .set(Post::getStatus, Post.STATUS_DELETED)
        );
    }

    @Override
    public PostVO getPostDetail(Long postId) {
        Post post = getPostOrThrow(postId);

        // 已删除的帖子对普通用户不可见
        if (Integer.valueOf(Post.STATUS_DELETED).equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "帖子不存在");
        }

        // 草稿只有作者本人可见
        if (Integer.valueOf(Post.STATUS_DRAFT).equals(post.getStatus())) {
            Long currentUserId = UserContext.getUserId();
            if (!post.getUserId().equals(currentUserId)) {
                throw new BusinessException(ErrorCode.NOT_FOUND.code(), "帖子不存在");
            }
        }

        // 浏览量 +1（异步效果：直接更新，不等待返回）
        postMapper.incrementViewCount(postId);

        PostVO vo = PostVO.from(post);
        // 填充作者信息
        fillAuthorInfo(vo);
        // 填充分区名称
        fillSectionName(vo);
        // 填充当前用户的交互状态（点赞、收藏、关注、拉黑）
        fillUserInteractionStatus(vo);
        return vo;
    }

    @Override
    public PageVO<PostVO> listPosts(Long sectionId, Long userId, int page, int pageSize) {
        // 限制最大页大小，防止一次查太多
        pageSize = Math.min(pageSize, 50);

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, Post.STATUS_NORMAL)
                .orderByDesc(Post::getCreatedAt);

        if (sectionId != null) {
            wrapper.eq(Post::getSectionId, sectionId);
        }
        // 指定用户时仅返回该用户的公开帖子（个人首页用）
        if (userId != null) {
            wrapper.eq(Post::getUserId, userId);
        }

        Page<Post> pageResult = postMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<PostVO> voList = pageResult.getRecords().stream()
                .map(post -> {
                    PostVO vo = PostVO.from(post);
                    // 列表页不返回正文内容，减少流量
                    vo.setContent(null);
                    return vo;
                })
                .collect(Collectors.toList());

        // 批量填充作者和分区信息
        fillBatchInfo(voList);

        return PageVO.of(pageResult, voList);
    }

    @Override
    public PageVO<PostVO> listMyPosts(Long userId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);

        // 查询自己的帖子（包含草稿，但不含已删除）
        Page<Post> pageResult = postMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)
                        .ne(Post::getStatus, Post.STATUS_DELETED)
                        .orderByDesc(Post::getCreatedAt)
        );

        List<PostVO> voList = pageResult.getRecords().stream()
                .map(post -> {
                    PostVO vo = PostVO.from(post);
                    vo.setContent(null);
                    return vo;
                })
                .collect(Collectors.toList());

        fillBatchInfo(voList);
        return PageVO.of(pageResult, voList);
    }

    // ===== 私有辅助方法 =====

    private Post getPostOrThrow(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "帖子不存在");
        }
        return post;
    }

    /** 校验当前用户是作者本人或管理员 */
    private void checkOwnerOrAdmin(Long currentUserId, Long ownerId) {
        boolean isAdmin = UserContext.get() != null && UserContext.get().isAdmin();
        if (!currentUserId.equals(ownerId) && !isAdmin) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "无权限操作此帖子");
        }
    }

    /** 检查用户是否被禁言（禁言和封号都不能发帖） */
    private void checkUserNotMuted(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && !Integer.valueOf(User.STATUS_NORMAL).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "账号已被限制，无法发帖");
        }
    }

    /** 将图片列表序列化为JSON字符串 */
    private String serializeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return new ObjectMapper().writeValueAsString(images);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.code(), "图片数据序列化失败");
        }
    }

    /** 将标签列表序列化为JSON字符串 */
    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        // 标签去重并过滤空标签
        List<String> validTags = tags.stream()
                .filter(t -> t != null && !t.trim().isEmpty())
                .distinct()
                .limit(5) // 最多5个标签
                .collect(Collectors.toList());
        if (validTags.isEmpty()) {
            return null;
        }
        try {
            return new ObjectMapper().writeValueAsString(validTags);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.code(), "标签数据序列化失败");
        }
    }

    /** 填充单个 VO 的作者信息 */
    private void fillAuthorInfo(PostVO vo) {
        User author = userMapper.selectById(vo.getUserId());
        if (author != null) {
            vo.setAuthorName(author.getNickname());
            vo.setAuthorAvatar(author.getAvatarUrl());
        }
    }

    /** 填充单个 VO 的分区名称 */
    private void fillSectionName(PostVO vo) {
        Section section = sectionMapper.selectById(vo.getSectionId());
        if (section != null) {
            vo.setSectionName(section.getName());
        }
    }

    /** 批量填充作者信息和分区名称（减少 N+1 查询） */
    private void fillBatchInfo(List<PostVO> voList) {
        if (voList.isEmpty()) return;

        // 批量查作者
        Set<Long> userIds = voList.stream().map(PostVO::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        // 批量查分区
        Set<Long> sectionIds = voList.stream().map(PostVO::getSectionId).collect(Collectors.toSet());
        Map<Long, Section> sectionMap = sectionMapper.selectBatchIds(sectionIds)
                .stream().collect(Collectors.toMap(Section::getId, s -> s));

        voList.forEach(vo -> {
            User author = userMap.get(vo.getUserId());
            if (author != null) {
                vo.setAuthorName(author.getNickname());
                vo.setAuthorAvatar(author.getAvatarUrl());
            }
            Section section = sectionMap.get(vo.getSectionId());
            if (section != null) {
                vo.setSectionName(section.getName());
            }
        });
    }

    /** 填充当前用户对帖子的交互状态（点赞、收藏、关注、拉黑） */
    private void fillUserInteractionStatus(PostVO vo) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            return;
        }

        // 不能查看自己是否被拉黑（没有意义）
        if (currentUserId.equals(vo.getUserId())) {
            return;
        }

        // 查询当前用户是否点赞了此帖子
        Long postId = vo.getId();
        Long authorId = vo.getUserId();

        // 点赞状态
        com.ttikss.nexatalk.entity.PostLike like = postLikeMapper.selectOne(
                new LambdaQueryWrapper<com.ttikss.nexatalk.entity.PostLike>()
                        .eq(com.ttikss.nexatalk.entity.PostLike::getUserId, currentUserId)
                        .eq(com.ttikss.nexatalk.entity.PostLike::getPostId, postId)
        );
        vo.setIsLiked(like != null);

        // 收藏状态
        com.ttikss.nexatalk.entity.Favorite favorite = favoriteMapper.selectOne(
                new LambdaQueryWrapper<com.ttikss.nexatalk.entity.Favorite>()
                        .eq(com.ttikss.nexatalk.entity.Favorite::getUserId, currentUserId)
                        .eq(com.ttikss.nexatalk.entity.Favorite::getPostId, postId)
        );
        vo.setIsFavorited(favorite != null);

        // 关注状态
        com.ttikss.nexatalk.entity.Follow follow = followMapper.selectOne(
                new LambdaQueryWrapper<com.ttikss.nexatalk.entity.Follow>()
                        .eq(com.ttikss.nexatalk.entity.Follow::getFollowerId, currentUserId)
                        .eq(com.ttikss.nexatalk.entity.Follow::getFolloweeId, authorId)
        );
        vo.setIsFollowed(follow != null);

        // 拉黑状态：检查当前用户是否被作者拉黑
        com.ttikss.nexatalk.entity.Blacklist blocked = blacklistMapper.selectOne(
                new LambdaQueryWrapper<com.ttikss.nexatalk.entity.Blacklist>()
                        .eq(com.ttikss.nexatalk.entity.Blacklist::getUserId, authorId)
                        .eq(com.ttikss.nexatalk.entity.Blacklist::getBlockedUserId, currentUserId)
        );
        vo.setIsBlocked(blocked != null);
    }

    @Override
    public String uploadImage(MultipartFile file) {
        final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
        try {
            return fileUtils.uploadFile(file, "post", MAX_IMAGE_SIZE);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.code(), "图片上传失败");
        }
    }

    @Override
    public void sharePost(Long postId) {
        Post post = getPostOrThrow(postId);
        // 已删除的帖子不能分享
        if (Integer.valueOf(Post.STATUS_DELETED).equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "帖子不存在");
        }
        // 分享数 +1
        postMapper.incrementShareCount(postId);
    }

    @Override
    public long countUserPosts(Long userId) {
        Long count = postMapper.selectCount(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)
                        .ne(Post::getStatus, Post.STATUS_DELETED)
        );
        return count != null ? count : 0;
    }
}
