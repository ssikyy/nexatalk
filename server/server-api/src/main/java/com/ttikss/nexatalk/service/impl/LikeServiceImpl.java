package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.entity.Comment;
import com.ttikss.nexatalk.entity.CommentLike;
import com.ttikss.nexatalk.entity.Post;
import com.ttikss.nexatalk.entity.PostLike;
import com.ttikss.nexatalk.entity.Section;
import com.ttikss.nexatalk.entity.Notification;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.CommentLikeMapper;
import com.ttikss.nexatalk.mapper.CommentMapper;
import com.ttikss.nexatalk.mapper.PostLikeMapper;
import com.ttikss.nexatalk.mapper.PostMapper;
import com.ttikss.nexatalk.mapper.SectionMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.service.LikeService;
import com.ttikss.nexatalk.service.NotificationService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PostVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 点赞模块业务实现
 *
 * 并发安全策略：
 * - 先查后写改为直接写，利用数据库唯一约束拦截重复插入
 * - 捕获 DuplicateKeyException 实现幂等（已点赞再点视为成功）
 */
@Service
public class LikeServiceImpl implements LikeService {

    private final PostLikeMapper postLikeMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final SectionMapper sectionMapper;
    private final NotificationService notificationService;

    public LikeServiceImpl(PostLikeMapper postLikeMapper,
                           CommentLikeMapper commentLikeMapper,
                           PostMapper postMapper,
                           CommentMapper commentMapper,
                           UserMapper userMapper,
                           SectionMapper sectionMapper,
                           NotificationService notificationService) {
        this.postLikeMapper = postLikeMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
        this.sectionMapper = sectionMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void likePost(Long userId, Long postId) {
        // 检查帖子是否存在
        Post post = postMapper.selectById(postId);
        if (post == null || Integer.valueOf(Post.STATUS_DELETED).equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "帖子不存在");
        }

        try {
            // 直接插入，依赖数据库唯一约束防重复（比先查后写更安全）
            PostLike like = new PostLike();
            like.setUserId(userId);
            like.setPostId(postId);
            postLikeMapper.insert(like);
            // 帖子点赞数 +1（原子操作）
            postMapper.updateLikeCount(postId, 1);

            // 发送通知给帖子作者
            if (!post.getUserId().equals(userId)) {
                notificationService.send(
                        post.getUserId(),
                        userId,
                        Notification.TYPE_LIKE_POST,
                        Notification.ENTITY_TYPE_POST,
                        postId,
                        "点赞了你的帖子"
                );
            }
        } catch (DuplicateKeyException e) {
            // 已点赞，幂等处理：忽略重复插入，不报错
        }
    }

    @Override
    @Transactional
    public void unlikePost(Long userId, Long postId) {
        int deleted = postLikeMapper.delete(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getUserId, userId)
                        .eq(PostLike::getPostId, postId)
        );
        // 只有实际删除了记录，才更新计数（防止计数出现负数）
        if (deleted > 0) {
            postMapper.updateLikeCount(postId, -1);
        }
    }

    @Override
    public boolean hasLikedPost(Long userId, Long postId) {
        Long count = postLikeMapper.selectCount(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getUserId, userId)
                        .eq(PostLike::getPostId, postId)
        );
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public void likeComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || Integer.valueOf(Comment.STATUS_DELETED).equals(comment.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "评论不存在");
        }

        try {
            CommentLike like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            commentLikeMapper.insert(like);
            commentMapper.updateLikeCount(commentId, 1);

            // 发送通知给评论作者
            if (!comment.getUserId().equals(userId)) {
                notificationService.send(
                        comment.getUserId(),
                        userId,
                        Notification.TYPE_LIKE_COMMENT,
                        Notification.ENTITY_TYPE_COMMENT,
                        commentId,
                        "点赞了你的评论"
                );
            }
        } catch (DuplicateKeyException e) {
            // 已点赞，幂等处理
        }
    }

    @Override
    @Transactional
    public void unlikeComment(Long userId, Long commentId) {
        int deleted = commentLikeMapper.delete(
                new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getUserId, userId)
                        .eq(CommentLike::getCommentId, commentId)
        );
        if (deleted > 0) {
            commentMapper.updateLikeCount(commentId, -1);
        }
    }

    @Override
    public boolean hasLikedComment(Long userId, Long commentId) {
        Long count = commentLikeMapper.selectCount(
                new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getUserId, userId)
                        .eq(CommentLike::getCommentId, commentId)
        );
        return count != null && count > 0;
    }

    @Override
    public PageVO<PostVO> listMyLikedPosts(Long userId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);

        Page<PostLike> likePage = postLikeMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getUserId, userId)
                        .orderByDesc(PostLike::getCreatedAt)
        );

        if (likePage.getRecords().isEmpty()) {
            return PageVO.of(likePage, Collections.emptyList());
        }

        List<Long> postIds = likePage.getRecords().stream()
                .map(PostLike::getPostId)
                .collect(Collectors.toList());

        List<Post> posts = postMapper.selectBatchIds(postIds);
        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        Set<Long> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        Set<Long> sectionIds = posts.stream().map(Post::getSectionId).collect(Collectors.toSet());
        Map<Long, Section> sectionMap = sectionMapper.selectBatchIds(sectionIds)
                .stream().collect(Collectors.toMap(Section::getId, s -> s));

        List<PostVO> voList = postIds.stream()
                .map(postMap::get)
                .filter(p -> p != null && !Integer.valueOf(Post.STATUS_DELETED).equals(p.getStatus()))
                .map(p -> {
                    PostVO vo = PostVO.from(p);
                    vo.setContent(null);
                    // 当前用户点赞的列表，每条都应该是已点赞状态
                    vo.setIsLiked(true);
                    User author = userMap.get(p.getUserId());
                    if (author != null) {
                        vo.setAuthorName(author.getNickname());
                        vo.setAuthorAvatar(author.getAvatarUrl());
                    }
                    Section section = sectionMap.get(p.getSectionId());
                    if (section != null) vo.setSectionName(section.getName());
                    return vo;
                })
                .collect(Collectors.toList());

        return PageVO.of(likePage, voList);
    }

    @Override
    public long countUserLiked(Long userId) {
        // 查询该用户发布的所有帖子的点赞总数
        // 需要先查出用户的所有帖子，然后统计这些帖子的点赞数
        List<Post> userPosts = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getUserId, userId)
                        .ne(Post::getStatus, Post.STATUS_DELETED)
        );

        if (userPosts.isEmpty()) {
            return 0L;
        }

        // 统计所有帖子的点赞数
        long totalLiked = 0L;
        for (Post post : userPosts) {
            totalLiked += (post.getLikeCount() != null ? post.getLikeCount() : 0);
        }
        return totalLiked;
    }
}
