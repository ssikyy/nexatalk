package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.entity.Favorite;
import com.ttikss.nexatalk.entity.Post;
import com.ttikss.nexatalk.entity.Section;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.FavoriteMapper;
import com.ttikss.nexatalk.mapper.PostMapper;
import com.ttikss.nexatalk.mapper.SectionMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.service.FavoriteService;
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
 * 收藏模块业务实现
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final SectionMapper sectionMapper;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper,
                                PostMapper postMapper,
                                UserMapper userMapper,
                                SectionMapper sectionMapper) {
        this.favoriteMapper = favoriteMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.sectionMapper = sectionMapper;
    }

    @Override
    @Transactional
    public void favorite(Long userId, Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || Integer.valueOf(Post.STATUS_DELETED).equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "帖子不存在");
        }
        try {
            Favorite fav = new Favorite();
            fav.setUserId(userId);
            fav.setPostId(postId);
            favoriteMapper.insert(fav);
            postMapper.updateFavoriteCount(postId, 1);
        } catch (DuplicateKeyException e) {
            // 已收藏，幂等处理
        }
    }

    @Override
    @Transactional
    public void unfavorite(Long userId, Long postId) {
        int deleted = favoriteMapper.delete(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getPostId, postId)
        );
        if (deleted > 0) {
            postMapper.updateFavoriteCount(postId, -1);
        }
    }

    @Override
    public boolean hasFavorited(Long userId, Long postId) {
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getPostId, postId)
        );
        return count != null && count > 0;
    }

    @Override
    public PageVO<PostVO> listMyFavorites(Long userId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);

        Page<Favorite> favPage = favoriteMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreatedAt)
        );

        if (favPage.getRecords().isEmpty()) {
            return PageVO.of(favPage, Collections.emptyList());
        }

        List<Long> postIds = favPage.getRecords().stream()
                .map(Favorite::getPostId)
                .collect(Collectors.toList());

        List<Post> posts = postMapper.selectBatchIds(postIds);
        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        // 批量查作者/分区
        Set<Long> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        Set<Long> sectionIds = posts.stream().map(Post::getSectionId).collect(Collectors.toSet());
        Map<Long, Section> sectionMap = sectionMapper.selectBatchIds(sectionIds)
                .stream().collect(Collectors.toMap(Section::getId, s -> s));

        // 按收藏顺序排列，过滤已删除帖子
        List<PostVO> voList = postIds.stream()
                .map(postMap::get)
                .filter(p -> p != null && !Integer.valueOf(Post.STATUS_DELETED).equals(p.getStatus()))
                .map(p -> {
                    PostVO vo = PostVO.from(p);
                    vo.setContent(null);
                    // 当前用户收藏的列表，每条都应该是已收藏状态
                    vo.setIsFavorited(true);
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

        return PageVO.of(favPage, voList);
    }
}
