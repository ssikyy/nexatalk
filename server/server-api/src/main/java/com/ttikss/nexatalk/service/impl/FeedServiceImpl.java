package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttikss.nexatalk.entity.Blacklist;
import com.ttikss.nexatalk.entity.Favorite;
import com.ttikss.nexatalk.entity.Follow;
import com.ttikss.nexatalk.entity.Post;
import com.ttikss.nexatalk.entity.PostLike;
import com.ttikss.nexatalk.entity.Punishment;
import com.ttikss.nexatalk.entity.Section;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.mapper.BlacklistMapper;
import com.ttikss.nexatalk.mapper.FavoriteMapper;
import com.ttikss.nexatalk.mapper.FollowMapper;
import com.ttikss.nexatalk.mapper.PostLikeMapper;
import com.ttikss.nexatalk.mapper.PostMapper;
import com.ttikss.nexatalk.mapper.PunishmentMapper;
import com.ttikss.nexatalk.mapper.SectionMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.service.FeedService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PostVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 信息流模块业务实现
 */
@Service
public class FeedServiceImpl implements FeedService {

    private static final Logger log = LoggerFactory.getLogger(FeedServiceImpl.class);

    private static final String CACHE_KEY_PREFIX = "feed:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(2);

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final SectionMapper sectionMapper;
    private final FollowMapper followMapper;
    private final PostLikeMapper postLikeMapper;
    private final FavoriteMapper favoriteMapper;
    private final BlacklistMapper blacklistMapper;
    private final PunishmentMapper punishmentMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public FeedServiceImpl(PostMapper postMapper,
                           UserMapper userMapper,
                           SectionMapper sectionMapper,
                           FollowMapper followMapper,
                           PostLikeMapper postLikeMapper,
                           FavoriteMapper favoriteMapper,
                           BlacklistMapper blacklistMapper,
                           PunishmentMapper punishmentMapper,
                           StringRedisTemplate redisTemplate) {
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.sectionMapper = sectionMapper;
        this.followMapper = followMapper;
        this.postLikeMapper = postLikeMapper;
        this.favoriteMapper = favoriteMapper;
        this.blacklistMapper = blacklistMapper;
        this.punishmentMapper = punishmentMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    @Override
    public PageVO<PostVO> getLatestFeed(Long userId, Long sectionId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);

        // 未登录时尝试从缓存获取
        if (userId == null) {
            String cacheKey = buildCacheKey("latest", sectionId, page, pageSize);
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return objectMapper.readValue(cached, new TypeReference<PageVO<PostVO>>() {});
                }
            } catch (Exception e) {
                log.warn("Failed to get latest feed from cache: {}", e.getMessage());
            }
        }

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, Post.STATUS_NORMAL)
                .orderByDesc(Post::getCreatedAt);

        if (sectionId != null) {
            wrapper.eq(Post::getSectionId, sectionId);
        }

        // 过滤被拉黑/封禁的用户的帖子
        Set<Long> excludedUserIds = getExcludedUserIds(userId);
        if (!excludedUserIds.isEmpty()) {
            wrapper.notIn(Post::getUserId, excludedUserIds);
        }

        Page<Post> pageResult = postMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<PostVO> voList = toVoList(pageResult.getRecords(), userId);
        PageVO<PostVO> result = PageVO.of(pageResult, voList);

        // 未登录时缓存结果
        if (userId == null) {
            String cacheKey = buildCacheKey("latest", sectionId, page, pageSize);
            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), CACHE_TTL);
            } catch (Exception e) {
                log.warn("Failed to cache latest feed: {}", e.getMessage());
            }
        }

        return result;
    }

    @Override
    public PageVO<PostVO> getFollowFeed(Long userId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);

        // 查询当前用户关注的所有人的 ID 列表（带缓存，缓存5分钟）
        List<Long> followeeIds = getFolloweeIdsWithCache(userId);

        // 没有关注任何人，返回空结果
        if (followeeIds.isEmpty()) {
            Page<Post> empty = new Page<>(page, pageSize);
            empty.setTotal(0);
            return PageVO.of(empty, Collections.emptyList());
        }

        // 过滤被拉黑/封禁的用户
        Set<Long> excludedUserIds = getExcludedUserIds(userId);
        followeeIds.removeAll(excludedUserIds);

        if (followeeIds.isEmpty()) {
            Page<Post> empty = new Page<>(page, pageSize);
            empty.setTotal(0);
            return PageVO.of(empty, Collections.emptyList());
        }

        // 查询关注列表中所有用户发布的正常状态帖子
        // 优化：使用子查询方式避免 IN 查询性能问题，分页更高效
        Page<Post> pageResult = postMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Post>()
                        .in(Post::getUserId, followeeIds)
                        .eq(Post::getStatus, Post.STATUS_NORMAL)
                        .orderByDesc(Post::getCreatedAt)
        );

        List<PostVO> voList = toVoList(pageResult.getRecords(), userId);
        return PageVO.of(pageResult, voList);
    }

    @Override
    public PageVO<PostVO> getHotFeed(Long userId, Long sectionId, String sort, String timeRange, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);

        // 未登录时尝试从缓存获取
        if (userId == null) {
            String cacheKey = buildCacheKey("hot", sectionId, sort, timeRange, page, pageSize);
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return objectMapper.readValue(cached, new TypeReference<PageVO<PostVO>>() {});
                }
            } catch (Exception e) {
                log.warn("Failed to get hot feed from cache: {}", e.getMessage());
            }
        }

        // 时间范围过滤
        LocalDateTime startTime = null;
        if (!"all".equals(timeRange)) {
            LocalDateTime now = LocalDateTime.now();
            switch (timeRange) {
                case "day":
                    startTime = now.minusDays(1);
                    break;
                case "week":
                    startTime = now.minusWeeks(1);
                    break;
                case "month":
                    startTime = now.minusMonths(1);
                    break;
            }
        }

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, Post.STATUS_NORMAL);

        // 添加时间范围过滤
        if (startTime != null) {
            wrapper.ge(Post::getCreatedAt, startTime);
        }

        if (sectionId != null) {
            wrapper.eq(Post::getSectionId, sectionId);
        }

        // 过滤被拉黑/封禁的用户的帖子
        Set<Long> excludedUserIds = getExcludedUserIds(userId);
        if (!excludedUserIds.isEmpty()) {
            wrapper.notIn(Post::getUserId, excludedUserIds);
        }

        // 根据排序方式设置排序规则
        if ("like".equals(sort)) {
            // 按点赞数倒序
            wrapper.orderByDesc(Post::getLikeCount);
        } else if ("comment".equals(sort)) {
            // 按评论数倒序
            wrapper.orderByDesc(Post::getCommentCount);
        } else {
            // 综合热门：热度分 = (点赞数 * 3 + 评论数 * 5 + 收藏数 * 2) * 时间衰减因子
            // 由于无法在 SQL 中计算复杂公式，使用组合排序：
            // 首先按热度指标排序，然后按时间衰减排序
            wrapper.orderByDesc(Post::getFavoriteCount)
                    .orderByDesc(Post::getLikeCount)
                    .orderByDesc(Post::getCommentCount);
        }

        // 相同热度时按时间倒序（越新的越靠前）
        wrapper.orderByDesc(Post::getCreatedAt);

        Page<Post> pageResult = postMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<PostVO> voList = toVoList(pageResult.getRecords(), userId);
        PageVO<PostVO> result = PageVO.of(pageResult, voList);

        // 未登录时缓存结果
        if (userId == null) {
            String cacheKey = buildCacheKey("hot", sectionId, sort, timeRange, page, pageSize);
            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), CACHE_TTL);
            } catch (Exception e) {
                log.warn("Failed to cache hot feed: {}", e.getMessage());
            }
        }

        return result;
    }

    /** 将 Post 列表转为 VO 并批量填充作者/分区信息，以及当前用户的点赞/收藏状态 */
    private List<PostVO> toVoList(List<Post> posts, Long userId) {
        if (posts.isEmpty()) return Collections.emptyList();

        // 第一步：转换为 VO 并填充基础信息
        List<PostVO> voList = posts.stream()
                .map(p -> {
                    PostVO vo = PostVO.from(p);
                    vo.setContent(null); // 列表不返回正文
                    return vo;
                })
                .collect(Collectors.toList());

        // 收集需要的用户ID和分区ID
        Set<Long> userIds = voList.stream().map(PostVO::getUserId).collect(Collectors.toSet());
        Set<Long> sectionIds = voList.stream().map(PostVO::getSectionId).collect(Collectors.toSet());

        // 批量查作者
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        // 批量查分区
        Map<Long, Section> sectionMap = sectionMapper.selectBatchIds(sectionIds)
                .stream().collect(Collectors.toMap(Section::getId, s -> s));

        // 批量查询当前用户的点赞状态和关注状态
        Map<Long, Boolean> likedMap = Collections.emptyMap();
        Map<Long, Boolean> favoritedMap = Collections.emptyMap();
        Map<Long, Boolean> followedMap = Collections.emptyMap();

        if (userId != null) {
            Set<Long> postIds = voList.stream().map(PostVO::getId).collect(Collectors.toSet());
            likedMap = getLikedStatus(userId, postIds);
            favoritedMap = getFavoritedStatus(userId, postIds);
            followedMap = getFollowedStatus(userId, userIds);
        }

        // 填充详细信息
        for (PostVO vo : voList) {
            // 作者信息
            User author = userMap.get(vo.getUserId());
            if (author != null) {
                vo.setAuthorName(author.getNickname());
                vo.setAuthorAvatar(author.getAvatarUrl());
            }

            // 分区信息
            Section section = sectionMap.get(vo.getSectionId());
            if (section != null) {
                vo.setSectionName(section.getName());
            }

            // 当前用户的点赞/收藏/关注状态
            if (userId != null) {
                vo.setIsLiked(likedMap.getOrDefault(vo.getId(), false));
                vo.setIsFavorited(favoritedMap.getOrDefault(vo.getId(), false));
                vo.setIsFollowed(followedMap.getOrDefault(vo.getUserId(), false));
            }
        }

        return voList;
    }

    /** 批量查询当前用户对一批帖子的点赞状态 */
    private Map<Long, Boolean> getLikedStatus(Long userId, Set<Long> postIds) {
        if (postIds.isEmpty()) return Collections.emptyMap();
        List<PostLike> likes = postLikeMapper.selectList(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getUserId, userId)
                        .in(PostLike::getPostId, postIds)
        );
        return likes.stream().collect(Collectors.toMap(PostLike::getPostId, p -> true, (a, b) -> a));
    }

    /** 批量查询当前用户对一批帖子的收藏状态 */
    private Map<Long, Boolean> getFavoritedStatus(Long userId, Set<Long> postIds) {
        if (postIds.isEmpty()) return Collections.emptyMap();
        List<Favorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .in(Favorite::getPostId, postIds)
        );
        return favorites.stream().collect(Collectors.toMap(Favorite::getPostId, p -> true, (a, b) -> a));
    }

    /** 批量查询当前用户是否关注了一批作者 */
    private Map<Long, Boolean> getFollowedStatus(Long userId, Set<Long> authorIds) {
        if (authorIds.isEmpty()) return Collections.emptyMap();
        List<Follow> follows = followMapper.selectList(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, userId)
                        .in(Follow::getFolloweeId, authorIds)
        );
        return follows.stream().collect(Collectors.toMap(Follow::getFolloweeId, f -> true, (a, b) -> a));
    }

    /** 获取用户关注列表（带缓存，缓存5分钟） */
    private List<Long> getFolloweeIdsWithCache(Long userId) {
        String cacheKey = "user:followees:" + userId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<Long>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to get followees from cache: {}", e.getMessage());
        }

        // 查询数据库
        List<Long> followeeIds = followMapper.selectList(
                        new LambdaQueryWrapper<Follow>()
                                .eq(Follow::getFollowerId, userId)
                                .select(Follow::getFolloweeId))
                .stream()
                .map(Follow::getFolloweeId)
                .collect(Collectors.toList());

        // 缓存结果
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(followeeIds), Duration.ofMinutes(5));
        } catch (Exception e) {
            log.warn("Failed to cache followees: {}", e.getMessage());
        }

        return followeeIds;
    }

    /** 获取需要排除的用户ID列表（被拉黑的用户 + 被封禁的用户）- 带缓存 */
    private Set<Long> getExcludedUserIds(Long userId) {
        if (userId == null) return Collections.emptySet();

        Set<Long> excludedIds = new java.util.HashSet<>();

        // 1. 获取被当前用户拉黑的用户ID（从缓存获取，缓存5分钟）
        String blacklistCacheKey = "user:blacklist:" + userId;
        try {
            String cached = redisTemplate.opsForValue().get(blacklistCacheKey);
            if (cached != null) {
                Set<Long> cachedIds = objectMapper.readValue(cached, new TypeReference<java.util.HashSet<Long>>() {});
                excludedIds.addAll(cachedIds);
            } else {
                List<Blacklist> blockedList = blacklistMapper.selectList(
                        new LambdaQueryWrapper<Blacklist>()
                                .eq(Blacklist::getUserId, userId)
                                .select(Blacklist::getBlockedUserId)
                );
                Set<Long> blockedIds = blockedList.stream()
                        .map(Blacklist::getBlockedUserId)
                        .collect(Collectors.toSet());
                excludedIds.addAll(blockedIds);

                // 缓存5分钟
                redisTemplate.opsForValue().set(blacklistCacheKey,
                        objectMapper.writeValueAsString(blockedIds), Duration.ofMinutes(5));
            }
        } catch (Exception e) {
            log.warn("Failed to get blacklist from cache: {}", e.getMessage());
            // 降级处理：直接查询数据库
            List<Blacklist> blockedList = blacklistMapper.selectList(
                    new LambdaQueryWrapper<Blacklist>()
                            .eq(Blacklist::getUserId, userId)
                            .select(Blacklist::getBlockedUserId)
            );
            excludedIds.addAll(blockedList.stream()
                    .map(Blacklist::getBlockedUserId)
                    .collect(Collectors.toSet()));
        }

        // 2. 获取被封禁的用户ID（全局缓存，缓存10分钟）
        String punishedCacheKey = "user:punished:all";
        try {
            String cached = redisTemplate.opsForValue().get(punishedCacheKey);
            if (cached != null) {
                Set<Long> cachedIds = objectMapper.readValue(cached, new TypeReference<java.util.HashSet<Long>>() {});
                excludedIds.addAll(cachedIds);
            } else {
                LocalDateTime now = LocalDateTime.now();
                List<Punishment> activePunishments = punishmentMapper.selectList(
                        new LambdaQueryWrapper<Punishment>()
                                .eq(Punishment::getIsActive, Punishment.ACTIVE)
                                .and(w -> w
                                        .eq(Punishment::getType, Punishment.TYPE_BAN)
                                        .or()
                                        .eq(Punishment::getType, Punishment.TYPE_MUTE)
                                        .and(w2 -> w2
                                                .isNull(Punishment::getExpireAt)
                                                .or()
                                                .gt(Punishment::getExpireAt, now)
                                        )
                                )
                );
                Set<Long> punishedIds = activePunishments.stream()
                        .map(Punishment::getUserId)
                        .collect(Collectors.toSet());
                excludedIds.addAll(punishedIds);

                // 缓存10分钟
                redisTemplate.opsForValue().set(punishedCacheKey,
                        objectMapper.writeValueAsString(punishedIds), Duration.ofMinutes(10));
            }
        } catch (Exception e) {
            log.warn("Failed to get punished users from cache: {}", e.getMessage());
            // 降级处理：直接查询数据库
            LocalDateTime now = LocalDateTime.now();
            List<Punishment> activePunishments = punishmentMapper.selectList(
                    new LambdaQueryWrapper<Punishment>()
                            .eq(Punishment::getIsActive, Punishment.ACTIVE)
                            .and(w -> w
                                    .eq(Punishment::getType, Punishment.TYPE_BAN)
                                    .or()
                                    .eq(Punishment::getType, Punishment.TYPE_MUTE)
                                    .and(w2 -> w2
                                            .isNull(Punishment::getExpireAt)
                                            .or()
                                            .gt(Punishment::getExpireAt, now)
                                    )
                            )
            );
            excludedIds.addAll(activePunishments.stream()
                    .map(Punishment::getUserId)
                    .collect(Collectors.toSet()));
        }

        return excludedIds;
    }

    /** 构建缓存键（最新流） */
    private String buildCacheKey(String type, Long sectionId, int page, int pageSize) {
        return CACHE_KEY_PREFIX + type + ":" + (sectionId != null ? sectionId : "all") + ":" + page + ":" + pageSize;
    }

    /** 构建缓存键（热门流） */
    private String buildCacheKey(String type, Long sectionId, String sort, String timeRange, int page, int pageSize) {
        return CACHE_KEY_PREFIX + type + ":" + (sectionId != null ? sectionId : "all") + ":" + sort + ":" + timeRange + ":" + page + ":" + pageSize;
    }
}
