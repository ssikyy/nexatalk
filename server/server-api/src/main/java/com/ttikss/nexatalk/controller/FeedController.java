package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.security.UserContext;
import com.ttikss.nexatalk.service.FeedService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PostVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 信息流模块控制器
 *
 * 接口清单：
 * GET /api/feed/latest   全站最新流（复用 Post 列表逻辑，按时间倒序）
 * GET /api/feed/follow   关注流（只显示我关注的用户发的帖子，依赖 Follow 模块）
 * GET /api/feed/hot     热门流（按热度排序，支持多种排序方式）
 */
@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    /**
     * 全站最新流
     * 按发布时间倒序，可选分区过滤
     * 返回中会包含当前用户的点赞/收藏状态（如果已登录）
     */
    @GetMapping("/latest")
    public Result<PageVO<PostVO>> latestFeed(
            @RequestParam(required = false) Long sectionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        // 参数校验
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        pageSize = Math.min(pageSize, 50);

        // 从UserContext获取userId，未登录时为null（公开接口）
        Long userId = UserContext.getUserId();
        return Result.ok(feedService.getLatestFeed(userId, sectionId, page, pageSize));
    }

    /**
     * 关注流（需要登录）
     * 只返回当前用户关注的人发布的帖子，按时间倒序
     */
    @GetMapping("/follow")
    public Result<PageVO<PostVO>> followFeed(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        // 参数校验
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        pageSize = Math.min(pageSize, 50);

        // 关注流需要登录，从UserContext获取（由于拦截器软鉴权，这里需要强制校验）
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.ok(feedService.getFollowFeed(userId, page, pageSize));
    }

    /**
     * 热门流
     * 按热度排序，可选排序方式和分区过滤
     * sort 参数：hot(综合热门)、like(点赞最多)、comment(评论最多)
     * timeRange 参数：day(24小时)、week(7天)、month(30天)、all(全部)
     */
    @GetMapping("/hot")
    public Result<PageVO<PostVO>> hotFeed(
            @RequestParam(required = false) Long sectionId,
            @RequestParam(defaultValue = "hot") String sort,
            @RequestParam(defaultValue = "week") String timeRange,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        // 参数校验
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        pageSize = Math.min(pageSize, 50);

        // 校验 sort 参数
        if (!"hot".equals(sort) && !"like".equals(sort) && !"comment".equals(sort)) {
            sort = "hot";
        }
        // 校验 timeRange 参数
        if (!"day".equals(timeRange) && !"week".equals(timeRange)
            && !"month".equals(timeRange) && !"all".equals(timeRange)) {
            timeRange = "week";
        }

        // 从UserContext获取userId，未登录时为null（公开接口）
        Long userId = UserContext.getUserId();
        return Result.ok(feedService.getHotFeed(userId, sectionId, sort, timeRange, page, pageSize));
    }
}
