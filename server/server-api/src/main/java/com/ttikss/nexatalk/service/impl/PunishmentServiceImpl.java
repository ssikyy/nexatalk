package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.dto.PunishRequest;
import com.ttikss.nexatalk.entity.Punishment;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.PunishmentMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.service.OperationLogService;
import com.ttikss.nexatalk.service.PunishmentService;
import com.ttikss.nexatalk.vo.PunishmentVO;
import com.ttikss.nexatalk.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 处罚模块业务实现
 *
 * 权限联动逻辑：
 * 1. 下处罚 → 同步修改 user.status（禁言=1，封号=2）
 * 2. 解除处罚 → 重新计算该用户剩余生效处罚，避免错误恢复为正常
 * 3. UserServiceImpl.login() 已检查 user.status 决定能否登录
 * 4. PostServiceImpl / CommentServiceImpl 已检查 status=1 拒绝发帖/评论
 */
@Service
public class PunishmentServiceImpl implements PunishmentService {

    private final PunishmentMapper punishmentMapper;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;

    public PunishmentServiceImpl(PunishmentMapper punishmentMapper, UserMapper userMapper,
                                  OperationLogService operationLogService) {
        this.punishmentMapper = punishmentMapper;
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public void punish(Long operatorId, PunishRequest request) {
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "用户不存在");
        }
        // 不允许处罚管理员和超级管理员
        if (user.getRole() != null && (user.getRole() == User.ROLE_ADMIN || user.getRole() == User.ROLE_SUPER_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.code(), "不能处罚管理员");
        }

        // 计算到期时间
        LocalDateTime expireAt = null;
        if (request.getDurationHours() != null && request.getDurationHours() > 0) {
            expireAt = LocalDateTime.now().plusHours(request.getDurationHours());
        }

        Punishment punishment = new Punishment();
        punishment.setUserId(request.getUserId());
        punishment.setOperatorId(operatorId);
        punishment.setType(request.getType());
        punishment.setReason(request.getReason() != null ? request.getReason() : "");
        punishment.setExpireAt(expireAt);
        punishment.setIsActive(Punishment.ACTIVE);
        punishmentMapper.insert(punishment);

        // 联动更新用户状态，按当前所有生效处罚重新计算，避免把封号降级成禁言
        user.setStatus(resolveUserStatusFromActivePunishments(user.getId()));
        userMapper.updateById(user);

        // 记录操作日志
        String operationType = (request.getType() != null && request.getType() == Punishment.TYPE_MUTE)
                ? "禁言用户" : "封号用户";
        String targetUsername = user.getUsername();
        String reason = request.getReason() != null ? request.getReason() : "";
        String params = String.format("{\"userId\":%d,\"type\":%d,\"reason\":\"%s\",\"durationHours\":%d}",
                request.getUserId(), request.getType(), reason,
                request.getDurationHours() != null ? request.getDurationHours() : 0);

        operationLogService.log(
                operatorId,
                getOperatorUsername(operatorId),
                "punishments",
                operationType,
                "POST",
                "",
                params,
                "成功",
                1,
                null,
                null
        );
    }

    @Override
    @Transactional
    public void lift(Long operatorId, Long punishmentId) {
        Punishment punishment = punishmentMapper.selectById(punishmentId);
        if (punishment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "处罚记录不存在");
        }
        if (!Punishment.ACTIVE.equals(punishment.getIsActive())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "该处罚已解除");
        }

        // 将处罚记录标记为已解除
        punishment.setIsActive(Punishment.INACTIVE);
        punishmentMapper.updateById(punishment);

        // 联动恢复用户状态
        User user = userMapper.selectById(punishment.getUserId());
        if (user != null) {
            user.setStatus(resolveUserStatusFromActivePunishments(user.getId()));
            userMapper.updateById(user);
        }

        // 记录操作日志
        String operationType = (punishment.getType() != null && punishment.getType() == Punishment.TYPE_MUTE)
                ? "解除禁言" : "解除封号";
        User punishedUser = userMapper.selectById(punishment.getUserId());
        String targetUsername = punishedUser != null ? punishedUser.getUsername() : "";
        String params = String.format("{\"punishmentId\":%d,\"userId\":%d,\"type\":%d}",
                punishmentId, punishment.getUserId(), punishment.getType());

        operationLogService.log(
                operatorId,
                getOperatorUsername(operatorId),
                "punishments",
                operationType,
                "POST",
                "",
                params,
                "成功",
                1,
                null,
                null
        );
    }

    @Override
    public PageVO<PunishmentVO> listByUser(Long userId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);
        Page<Punishment> result = punishmentMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Punishment>()
                        .eq(Punishment::getUserId, userId)
                        .orderByDesc(Punishment::getCreatedAt)
        );

        List<PunishmentVO> voList = new ArrayList<>();
        for ( Punishment punishment : result.getRecords()) {
            voList.add(convertToVO(punishment));
        }
        return PageVO.of(result, voList);
    }

    @Override
    public PageVO<PunishmentVO> listActive(int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);
        Page<Punishment> result = punishmentMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Punishment>()
                        .eq(Punishment::getIsActive, Punishment.ACTIVE)
                        .orderByDesc(Punishment::getCreatedAt)
        );

        List<PunishmentVO> voList = new ArrayList<>();
        for ( Punishment punishment : result.getRecords()) {
            voList.add(convertToVO(punishment));
        }
        return PageVO.of(result, voList);
    }

    private PunishmentVO convertToVO(Punishment punishment) {
        PunishmentVO vo = new PunishmentVO();
        vo.setId(punishment.getId());
        vo.setUserId(punishment.getUserId());
        vo.setType(punishment.getType());
        vo.setTypeText(punishment.getType() != null && punishment.getType() == Punishment.TYPE_MUTE ? "禁言" : "封号");
        vo.setReason(punishment.getReason());
        vo.setIsActive(punishment.getIsActive());
        vo.setIsActiveText(punishment.getIsActive() != null && punishment.getIsActive() == Punishment.ACTIVE ? "生效中" : "已解除");
        vo.setExpireAt(punishment.getExpireAt());
        vo.setCreatedAt(punishment.getCreatedAt());

        // 填充用户信息
        if (punishment.getUserId() != null) {
            User user = userMapper.selectById(punishment.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname());
                vo.setUserUsername(user.getUsername());
            }
        }
        return vo;
    }

    private Integer resolveUserStatusFromActivePunishments(Long userId) {
        if (userId == null) {
            return User.STATUS_NORMAL;
        }

        List<Punishment> activePunishments = punishmentMapper.selectList(
                new LambdaQueryWrapper<Punishment>()
                        .eq(Punishment::getUserId, userId)
                        .eq(Punishment::getIsActive, Punishment.ACTIVE)
                        .and(query -> query.isNull(Punishment::getExpireAt)
                                .or()
                                .gt(Punishment::getExpireAt, LocalDateTime.now()))
                        .orderByDesc(Punishment::getCreatedAt)
        );

        boolean hasBan = activePunishments.stream()
                .anyMatch(item -> Punishment.TYPE_BAN.equals(item.getType()));
        if (hasBan) {
            return User.STATUS_BANNED;
        }

        boolean hasMute = activePunishments.stream()
                .anyMatch(item -> Punishment.TYPE_MUTE.equals(item.getType()));
        if (hasMute) {
            return User.STATUS_MUTED;
        }

        return User.STATUS_NORMAL;
    }

    private String getOperatorUsername(Long operatorId) {
        if (operatorId == null) {
            return "";
        }
        User operator = userMapper.selectById(operatorId);
        return operator != null ? operator.getUsername() : "";
    }
}
