package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.dto.CommentCreateRequest;
import com.ttikss.nexatalk.entity.Comment;
import com.ttikss.nexatalk.entity.Notification;
import com.ttikss.nexatalk.entity.Post;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.CommentMapper;
import com.ttikss.nexatalk.mapper.PostMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.security.UserContext;
import com.ttikss.nexatalk.service.CommentService;
import com.ttikss.nexatalk.service.NotificationService;
import com.ttikss.nexatalk.vo.CommentVO;
import com.ttikss.nexatalk.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论模块业务实现
 */
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public CommentServiceImpl(CommentMapper commentMapper,
                               PostMapper postMapper,
                               UserMapper userMapper,
                               NotificationService notificationService) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Long createComment(Long userId, CommentCreateRequest req) {
        // 检查用户账号状态（禁言用户不能评论）
        User user = userMapper.selectById(userId);
        if (user != null && !Integer.valueOf(User.STATUS_NORMAL).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "账号已被限制，无法评论");
        }

        // 检查帖子是否存在且正常
        Post post = postMapper.selectById(req.getPostId());
        if (post == null || Integer.valueOf(Post.STATUS_DELETED).equals(post.getStatus())
                || Integer.valueOf(Post.STATUS_HIDDEN).equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "帖子不存在");
        }

        Comment comment = new Comment();
        comment.setPostId(req.getPostId());
        comment.setUserId(userId);
        comment.setContent(req.getContent());
        comment.setStatus(Comment.STATUS_NORMAL);
        comment.setLikeCount(0);
        comment.setReplyCount(0);

        long parentId = req.getParentId() != null ? req.getParentId() : 0L;
        comment.setParentId(parentId);

        if (parentId == 0) {
            // 一级评论，rootId = 0（插入后没有意义，置 0）
            comment.setRootId(0L);
        } else {
            // 二级回复：找到父评论，确定 rootId
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || Integer.valueOf(Comment.STATUS_DELETED).equals(parent.getStatus())) {
                throw new BusinessException(ErrorCode.NOT_FOUND.code(), "被回复的评论不存在");
            }
            // 如果父评论是一级评论，rootId = 父评论 ID；否则 rootId 继承父评论的 rootId
            long rootId = (parent.getParentId() == 0) ? parent.getId() : parent.getRootId();
            comment.setRootId(rootId);
            // 根评论的 replyCount +1
            commentMapper.updateReplyCount(rootId, 1);
        }

        commentMapper.insert(comment);

        // 帖子评论数 +1
        postMapper.updateCommentCount(req.getPostId(), 1);

        // 发送通知
        sendCommentNotification(userId, post, parentId, comment);

        return comment.getId();
    }

    /** 发送评论通知 */
    private void sendCommentNotification(Long commenterId, Post post, long parentId, Comment comment) {
        // 1. 通知帖子作者有人评论了（如果不是自己评论自己的帖子）
        if (!post.getUserId().equals(commenterId)) {
            String content = "评论了你的帖子: " + (post.getTitle() != null ? post.getTitle() : "无标题");
            notificationService.send(
                    post.getUserId(),
                    commenterId,
                    Notification.TYPE_COMMENT,
                    Notification.ENTITY_TYPE_POST,
                    comment.getId(),
                    content
            );
        }

        // 2. 如果是回复评论，通知被回复的人
        if (parentId > 0) {
            Comment parentComment = commentMapper.selectById(parentId);
            if (parentComment != null && !parentComment.getUserId().equals(commenterId)
                    && !parentComment.getUserId().equals(post.getUserId())) {
                // 避免重复通知帖子作者
                String content = "回复了你的评论";
                notificationService.send(
                        parentComment.getUserId(),
                        commenterId,
                        Notification.TYPE_COMMENT,
                        Notification.ENTITY_TYPE_COMMENT,
                        comment.getId(),
                        content
                );
            }
        }
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "评论不存在");
        }

        // 作者或管理员可以删除
        boolean isAdmin = UserContext.get() != null && UserContext.get().isAdmin();
        if (!comment.getUserId().equals(userId) && !isAdmin) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "无权限删除此评论");
        }

        // 逻辑删除本条评论
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .eq(Comment::getId, commentId)
                .set(Comment::getStatus, Comment.STATUS_DELETED)
        );

        int totalDeleted = 1;
        Long postId = comment.getPostId();

        if (comment.getParentId() == 0) {
            // 一级评论：级联逻辑删除其下所有二级回复
            List<Comment> replies = commentMapper.selectList(
                    new LambdaQueryWrapper<Comment>()
                            .eq(Comment::getRootId, commentId)
                            .ne(Comment::getStatus, Comment.STATUS_DELETED)
            );
            if (!replies.isEmpty()) {
                for (Comment reply : replies) {
                    commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                            .eq(Comment::getId, reply.getId())
                            .set(Comment::getStatus, Comment.STATUS_DELETED)
                    );
                }
                totalDeleted += replies.size();
                int replyCount = comment.getReplyCount() != null ? comment.getReplyCount() : 0;
                commentMapper.updateReplyCount(commentId, -replyCount);
            }
        } else {
            // 二级回复：根评论 replyCount -1
            commentMapper.updateReplyCount(comment.getRootId(), -1);
        }

        // 帖子评论数减去本次删除的总数（1 条或 1 + 所有回复数）
        for (int i = 0; i < totalDeleted; i++) {
            postMapper.updateCommentCount(postId, -1);
        }
    }

    @Override
    public PageVO<CommentVO> listComments(Long postId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);

        // 查一级评论（parentId = 0，且未删除；已删除的一级评论及其下回复不再展示）
        Page<Comment> pageResult = commentMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .eq(Comment::getParentId, 0)
                        .ne(Comment::getStatus, Comment.STATUS_DELETED)
                        .orderByAsc(Comment::getCreatedAt)
        );

        List<CommentVO> voList = pageResult.getRecords().stream()
                .map(CommentVO::from)
                .collect(Collectors.toList());

        // 批量填充作者信息
        fillBatchAuthorInfo(voList);

        // 为每条一级评论预加载最新 3 条回复（仅未删除的）
        voList.forEach(vo -> {
            if (vo.getReplyCount() != null && vo.getReplyCount() > 0) {
                List<Comment> recentReplies = commentMapper.selectList(
                        new LambdaQueryWrapper<Comment>()
                                .eq(Comment::getRootId, vo.getId())
                                .ne(Comment::getStatus, Comment.STATUS_DELETED)
                                .orderByDesc(Comment::getCreatedAt)
                                .last("LIMIT 3")
                );
                List<CommentVO> replyVoList = recentReplies.stream()
                        .map(CommentVO::from)
                        .collect(Collectors.toList());
                fillBatchAuthorInfo(replyVoList);
                vo.setReplies(replyVoList);
            }
        });

        return PageVO.of(pageResult, voList);
    }

    @Override
    public PageVO<CommentVO> listReplies(Long rootId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);

        Page<Comment> pageResult = commentMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getRootId, rootId)
                        .ne(Comment::getStatus, Comment.STATUS_DELETED)
                        .orderByAsc(Comment::getCreatedAt)
        );

        List<CommentVO> voList = pageResult.getRecords().stream()
                .map(CommentVO::from)
                .collect(Collectors.toList());
        fillBatchAuthorInfo(voList);

        return PageVO.of(pageResult, voList);
    }

    /** 批量填充评论列表的作者信息 */
    private void fillBatchAuthorInfo(List<CommentVO> voList) {
        if (voList.isEmpty()) return;
        Set<Long> userIds = voList.stream().map(CommentVO::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        voList.forEach(vo -> {
            User author = userMap.get(vo.getUserId());
            if (author != null) {
                vo.setAuthorName(author.getNickname());
                vo.setAuthorAvatar(author.getAvatarUrl());
            }
        });
    }

    @Override
    public PageVO<CommentVO> listMyComments(Long userId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);

        Page<Comment> pageResult = commentMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, userId)
                        .ne(Comment::getStatus, Comment.STATUS_DELETED)
                        .orderByDesc(Comment::getCreatedAt)
        );

        List<CommentVO> voList = pageResult.getRecords().stream()
                .map(CommentVO::from)
                .collect(Collectors.toList());

        // 批量填充作者信息
        fillBatchAuthorInfo(voList);

        // 补充帖子信息
        Set<Long> postIds = voList.stream().map(CommentVO::getPostId).collect(Collectors.toSet());
        Map<Long, Post> postMap = postMapper.selectBatchIds(postIds)
                .stream().collect(Collectors.toMap(Post::getId, p -> p));
        voList.forEach(vo -> {
            Post post = postMap.get(vo.getPostId());
            if (post != null) {
                vo.setPostTitle(post.getTitle());
            }
        });

        return PageVO.of(pageResult, voList);
    }
}
