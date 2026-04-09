package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.service.SearchService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PostVO;
import com.ttikss.nexatalk.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索模块控制器
 *
 * 接口清单：
 * GET /api/search/posts   搜索帖子（关键词匹配标题+正文）
 * GET /api/search/users   搜索用户（关键词匹配用户名+昵称）
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /** 搜索帖子 */
    @GetMapping("/posts")
    public Result<PageVO<PostVO>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(searchService.searchPosts(keyword, page, pageSize));
    }

    /** 搜索用户 */
    @GetMapping("/users")
    public Result<PageVO<UserVO>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(searchService.searchUsers(keyword, page, pageSize));
    }
}
