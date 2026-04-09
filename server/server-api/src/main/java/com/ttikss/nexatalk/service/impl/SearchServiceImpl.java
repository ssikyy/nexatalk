package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.entity.Post;
import com.ttikss.nexatalk.entity.Section;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.PostMapper;
import com.ttikss.nexatalk.mapper.SectionMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.service.SearchService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PostVO;
import com.ttikss.nexatalk.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 搜索模块业务实现（MySQL LIKE 方案）
 */
@Service
public class SearchServiceImpl implements SearchService {

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final SectionMapper sectionMapper;

    public SearchServiceImpl(PostMapper postMapper,
                            UserMapper userMapper,
                            SectionMapper sectionMapper) {
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.sectionMapper = sectionMapper;
    }

    @Override
    public PageVO<PostVO> searchPosts(String keyword, int page, int pageSize) {
        validateKeyword(keyword);
        pageSize = Math.min(pageSize, 50);

        String like = "%" + keyword.trim() + "%";
        Page<Post> pageResult = postMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, Post.STATUS_NORMAL)
                        .and(w -> w.like(Post::getTitle, like).or().like(Post::getContent, like))
                        .orderByDesc(Post::getCreatedAt)
        );

        if (pageResult.getRecords().isEmpty()) {
            return PageVO.of(pageResult, Collections.emptyList());
        }

        List<Post> posts = pageResult.getRecords();
        Set<Long> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        Set<Long> sectionIds = posts.stream().map(Post::getSectionId).collect(Collectors.toSet());
        Map<Long, Section> sectionMap = sectionMapper.selectBatchIds(sectionIds)
                .stream().collect(Collectors.toMap(Section::getId, s -> s));

        List<PostVO> voList = posts.stream().map(p -> {
            PostVO vo = PostVO.from(p);
            vo.setContent(null);
            User author = userMap.get(p.getUserId());
            if (author != null) {
                vo.setAuthorName(author.getNickname());
                vo.setAuthorAvatar(author.getAvatarUrl());
            }
            Section section = sectionMap.get(p.getSectionId());
            if (section != null) vo.setSectionName(section.getName());
            return vo;
        }).collect(Collectors.toList());

        return PageVO.of(pageResult, voList);
    }

    @Override
    public PageVO<UserVO> searchUsers(String keyword, int page, int pageSize) {
        validateKeyword(keyword);
        pageSize = Math.min(pageSize, 50);

        String like = "%" + keyword.trim() + "%";
        Page<User> pageResult = userMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<User>()
                        .and(w -> w.like(User::getUsername, like).or().like(User::getNickname, like))
                        .orderByDesc(User::getCreatedAt)
        );

        List<UserVO> voList = pageResult.getRecords().stream()
                .map(UserVO::from)
                .collect(Collectors.toList());

        return PageVO.of(pageResult, voList);
    }

    private void validateKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "搜索关键词不能为空");
        }
        if (keyword.trim().length() > 50) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "搜索关键词不超过 50 个字符");
        }
    }
}
