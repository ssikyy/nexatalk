package com.ttikss.nexatalk.service;

import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PostVO;
import com.ttikss.nexatalk.vo.UserVO;

/**
 * 搜索模块业务层接口
 *
 * 当前方案：MySQL LIKE 全文搜索
 * 升级路径：数据量增大后可替换为 Elasticsearch，接口层无需改动
 */
public interface SearchService {

    /**
     * 搜索帖子（标题 + 正文 LIKE 匹配）
     *
     * @param keyword  搜索关键词
     * @param page     页码
     * @param pageSize 每页大小
     */
    PageVO<PostVO> searchPosts(String keyword, int page, int pageSize);

    /**
     * 搜索用户（用户名 + 昵称 LIKE 匹配）
     *
     * @param keyword  搜索关键词
     * @param page     页码
     * @param pageSize 每页大小
     */
    PageVO<UserVO> searchUsers(String keyword, int page, int pageSize);
}
