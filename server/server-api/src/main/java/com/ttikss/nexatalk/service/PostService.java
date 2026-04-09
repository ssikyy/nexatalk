package com.ttikss.nexatalk.service;

import com.ttikss.nexatalk.dto.PostCreateRequest;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PostVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 帖子模块业务层接口
 */
public interface PostService {

    /**
     * 发布/保存草稿帖子
     *
     * @param userId 当前登录用户 ID
     * @param req    帖子内容（req.draft=true 保存草稿）
     * @return 新帖子 ID
     */
    Long createPost(Long userId, PostCreateRequest req);

    /**
     * 编辑帖子
     * 只有作者本人或管理员可以编辑
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     * @param req    更新内容
     */
    void updatePost(Long userId, Long postId, PostCreateRequest req);

    /**
     * 删除帖子（逻辑删除，status 改为 4）
     * 只有作者或管理员可以删除
     *
     * @param userId 当前登录用户 ID
     * @param postId 帖子 ID
     */
    void deletePost(Long userId, Long postId);

    /**
     * 获取帖子详情（同时浏览量 +1）
     *
     * @param postId 帖子 ID
     * @return 帖子详情 VO（含完整 content）
     */
    PostVO getPostDetail(Long postId);

    /**
     * 帖子列表（最新流，按发布时间倒序分页）
     *
     * @param sectionId 分区 ID（null 表示全站）
     * @param userId    用户 ID（null 表示不限；非空时仅返回该用户的公开帖子，用于个人首页）
     * @param page      页码（从 1 开始）
     * @param pageSize  每页大小（最大 50）
     * @return 分页结果（列表中的 VO 不含 content 全文）
     */
    PageVO<PostVO> listPosts(Long sectionId, Long userId, int page, int pageSize);

    /**
     * 查询当前用户自己的帖子（含草稿）
     *
     * @param userId   当前登录用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageVO<PostVO> listMyPosts(Long userId, int page, int pageSize);

    /**
     * 上传帖子图片
     *
     * @param file 图片文件
     * @return 图片访问 URL
     */
    String uploadImage(MultipartFile file);

    /**
     * 分享帖子（分享数 +1）
     *
     * @param postId 帖子 ID
     */
    void sharePost(Long postId);

    /**
     * 统计用户发布的帖子数量（仅统计公开状态的帖子）
     *
     * @param userId 用户 ID
     * @return 帖子数量
     */
    long countUserPosts(Long userId);
}
