package com.ttikss.nexatalk.service;

import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PostVO;

/**
 * 信息流模块业务层接口
 */
public interface FeedService {

    /**
     * 全站最新流：按发布时间倒序，可选分区过滤
     *
     * @param userId    当前登录用户 ID（用于填充点赞/收藏状态，可为 null）
     * @param sectionId 分区 ID（null 表示全站）
     * @param page      页码
     * @param pageSize  每页大小
     */
    PageVO<PostVO> getLatestFeed(Long userId, Long sectionId, int page, int pageSize);

    /**
     * 关注流：只返回当前用户关注的人发布的帖子
     *
     * @param userId   当前登录用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     */
    PageVO<PostVO> getFollowFeed(Long userId, int page, int pageSize);

    /**
     * 热门流：按热度排序，可选排序方式和分区过滤
     *
     * @param userId    当前登录用户 ID（用于填充点赞/收藏状态，可为 null）
     * @param sectionId 分区 ID（null 表示全站）
     * @param sort      排序方式：hot(热门)、like(点赞最多)、comment(评论最多)
     * @param timeRange 时间范围：day(24小时)、week(7天)、month(30天)、all(全部)
     * @param page      页码
     * @param pageSize  每页大小
     */
    PageVO<PostVO> getHotFeed(Long userId, Long sectionId, String sort, String timeRange, int page, int pageSize);
}
