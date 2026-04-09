package com.ttikss.nexatalk.service;

import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PostVO;

/**
 * 收藏模块业务层接口
 */
public interface FavoriteService {

    /** 收藏帖子（幂等） */
    void favorite(Long userId, Long postId);

    /** 取消收藏（幂等） */
    void unfavorite(Long userId, Long postId);

    /** 是否已收藏 */
    boolean hasFavorited(Long userId, Long postId);

    /** 查询我的收藏列表（分页） */
    PageVO<PostVO> listMyFavorites(Long userId, int page, int pageSize);
}
