package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.dto.PostCreateRequest;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.service.PostService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PostVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 帖子模块控制器
 *
 * 接口清单：
 * GET    /api/posts              帖子列表（最新流，可按分区过滤）
 * GET    /api/posts/{id}         帖子详情（+浏览量）
 * GET    /api/posts/mine         我的帖子（含草稿）
 * POST   /api/posts              发布/保存草稿
 * PUT    /api/posts/{id}         编辑帖子
 * DELETE /api/posts/{id}         删除帖子
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * 帖子列表（最新流），按发布时间倒序分页
     * 可选参数 sectionId 过滤特定分区；userId 指定某用户的公开帖子（用于个人首页）
     */
    @GetMapping
    public Result<PageVO<PostVO>> listPosts(
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(postService.listPosts(sectionId, userId, page, pageSize));
    }

    /**
     * 获取帖子详情，同时浏览量 +1
     */
    @GetMapping("/{id}")
    public Result<PostVO> getPostDetail(@PathVariable Long id) {
        return Result.ok(postService.getPostDetail(id));
    }

    /**
     * 查看我的帖子（含草稿，需登录）
     */
    @GetMapping("/mine")
    public Result<PageVO<PostVO>> listMyPosts(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(postService.listMyPosts(userId, page, pageSize));
    }

    /**
     * 发布帖子或保存草稿
     * draft=true 则保存为草稿，不公开
     */
    @PostMapping
    public Result<Long> createPost(@CurrentUser Long userId,
                                   @Valid @RequestBody PostCreateRequest req) {
        Long postId = postService.createPost(userId, req);
        return Result.ok(postId);
    }

    /**
     * 编辑帖子（作者或管理员）
     */
    @PutMapping("/{id}")
    public Result<String> updatePost(@CurrentUser Long userId,
                                     @PathVariable Long id,
                                     @Valid @RequestBody PostCreateRequest req) {
        postService.updatePost(userId, id, req);
        return Result.ok("更新成功");
    }

    /**
     * 删除帖子（逻辑删除，作者或管理员）
     */
    @DeleteMapping("/{id}")
    public Result<String> deletePost(@CurrentUser Long userId, @PathVariable Long id) {
        postService.deletePost(userId, id);
        return Result.ok("删除成功");
    }

    /**
     * 上传帖子图片（需登录）
     */
    @PostMapping("/images")
    public Result<String> uploadImage(@CurrentUser Long userId, @RequestParam("file") MultipartFile file) {
        String imageUrl = postService.uploadImage(file);
        return Result.ok(imageUrl);
    }

    /**
     * 分享帖子（分享数 +1）
     */
    @PostMapping("/{id}/share")
    public Result<String> sharePost(@PathVariable Long id) {
        postService.sharePost(id);
        return Result.ok("分享成功");
    }
}
