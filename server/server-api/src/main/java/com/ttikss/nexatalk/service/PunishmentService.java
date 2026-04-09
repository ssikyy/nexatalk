package com.ttikss.nexatalk.service;

import com.ttikss.nexatalk.dto.PunishRequest;
import com.ttikss.nexatalk.entity.Punishment;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.PunishmentVO;

/**
 * 处罚模块业务层接口
 *
 * 设计说明：
 * - 下处罚时同步更新 user.status（禁言=1，封号=2）
 * - 解除处罚时将 user.status 恢复为 0
 * - 定时任务（后续接入）负责到期自动解除
 */
public interface PunishmentService {

    /** 管理员下处罚（禁言/封号） */
    void punish(Long operatorId, PunishRequest request);

    /** 管理员手动解除处罚 */
    void lift(Long operatorId, Long punishmentId);

    /** 查询某用户的处罚历史（分页） */
    PageVO<PunishmentVO> listByUser(Long userId, int page, int pageSize);

    /** 管理员查询当前生效的所有处罚记录（分页） */
    PageVO<PunishmentVO> listActive(int page, int pageSize);
}
