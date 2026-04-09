package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.dto.PunishRequest;
import com.ttikss.nexatalk.dto.ReportCreateRequest;
import com.ttikss.nexatalk.dto.ReportReviewRequest;
import com.ttikss.nexatalk.entity.Comment;
import com.ttikss.nexatalk.entity.Post;
import com.ttikss.nexatalk.entity.Report;
import com.ttikss.nexatalk.entity.Notification;
import com.ttikss.nexatalk.entity.User;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.CommentMapper;
import com.ttikss.nexatalk.mapper.PostMapper;
import com.ttikss.nexatalk.mapper.ReportMapper;
import com.ttikss.nexatalk.mapper.UserMapper;
import com.ttikss.nexatalk.service.PunishmentService;
import com.ttikss.nexatalk.service.ReportService;
import com.ttikss.nexatalk.service.NotificationService;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.ReportVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 举报模块业务实现
 */
@Service
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final PunishmentService punishmentService;
    private final NotificationService notificationService;

    public ReportServiceImpl(ReportMapper reportMapper,
                              PostMapper postMapper,
                              CommentMapper commentMapper,
                              UserMapper userMapper,
                              PunishmentService punishmentService,
                              NotificationService notificationService) {
        this.reportMapper = reportMapper;
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
        this.punishmentService = punishmentService;
        this.notificationService = notificationService;
    }

    @Override
    public void createReport(Long reporterId, ReportCreateRequest request) {
        // 不能举报自己
        Long targetUserId = getTargetUserId(request.getEntityType(), request.getEntityId());
        if (targetUserId != null && targetUserId.equals(reporterId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "不能举报自己");
        }

        // 检查重复举报：同一用户对同一实体只能举报一次（待处理中的举报）
        Long existingReportId = checkDuplicateReport(reporterId, request.getEntityType(), request.getEntityId());
        if (existingReportId != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "您已举报过该内容，请勿重复提交");
        }

        // 检查被举报实体是否存在
        if (Report.ENTITY_TYPE_POST.equals(request.getEntityType())) {
            Post post = postMapper.selectById(request.getEntityId());
            if (post == null || Integer.valueOf(Post.STATUS_DELETED).equals(post.getStatus())) {
                throw new BusinessException(ErrorCode.NOT_FOUND.code(), "帖子不存在");
            }
        } else if (Report.ENTITY_TYPE_COMMENT.equals(request.getEntityType())) {
            Comment comment = commentMapper.selectById(request.getEntityId());
            if (comment == null || Integer.valueOf(Comment.STATUS_DELETED).equals(comment.getStatus())) {
                throw new BusinessException(ErrorCode.NOT_FOUND.code(), "评论不存在");
            }
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setEntityType(request.getEntityType());
        report.setEntityId(request.getEntityId());
        report.setReason(request.getReason());
        report.setDetail(request.getDetail() != null ? request.getDetail() : "");
        report.setStatus(Report.STATUS_PENDING);
        reportMapper.insert(report);
    }

    /**
     * 获取被举报内容的作者ID
     */
    private Long getTargetUserId(Integer entityType, Long entityId) {
        if (entityType == null || entityId == null) return null;
        if (Report.ENTITY_TYPE_POST.equals(entityType)) {
            Post post = postMapper.selectById(entityId);
            return post != null ? post.getUserId() : null;
        } else if (Report.ENTITY_TYPE_COMMENT.equals(entityType)) {
            Comment comment = commentMapper.selectById(entityId);
            return comment != null ? comment.getUserId() : null;
        }
        return null;
    }

    /**
     * 检查是否已存在待处理的举报
     */
    private Long checkDuplicateReport(Long reporterId, Integer entityType, Long entityId) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getEntityType, entityType)
                .eq(Report::getEntityId, entityId)
                .eq(Report::getStatus, Report.STATUS_PENDING);
        Report existing = reportMapper.selectOne(wrapper);
        return existing != null ? existing.getId() : null;
    }

    @Override
    public void reviewReport(Long adminId, Long reportId, ReportReviewRequest request) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "举报记录不存在");
        }
        if (!Report.STATUS_PENDING.equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "该举报已审核，不可重复操作");
        }

        // 获取被举报内容作者ID，用于发送通知
        Long targetUserId = getTargetUserId(report.getEntityType(), report.getEntityId());

        // 更新举报状态
        report.setStatus(request.getStatus());
        report.setReviewerId(adminId);
        report.setReviewNote(request.getReviewNote() != null ? request.getReviewNote() : "");
        report.setReviewedAt(LocalDateTime.now());
        reportMapper.updateById(report);

        // 发送审核结果通知给被举报人
        if (targetUserId != null) {
            sendAuditNotification(targetUserId, report, request);
        }

        // 如果判定违规
        if (Report.STATUS_VIOLATED.equals(request.getStatus())) {
            // 1. 隐藏内容（如果请求中指定）
            boolean shouldHide = request.getHideContent() != null && request.getHideContent();
            if (shouldHide) {
                hideReportedContent(report.getEntityType(), report.getEntityId());
            }

            // 2. 处罚被举报人（如果请求中指定）
            if (request.getPunishType() != null) {
                punishReportedUser(adminId, report, request);
            }
        }

        // 审核完成后，清理同一实体的其它举报记录，避免积累大量重复记录
        clearOtherReportsOfSameEntity(report);
    }

    /** 发送审核结果通知 */
    private void sendAuditNotification(Long targetUserId, Report report, ReportReviewRequest request) {
        String content;
        int type;

        if (Report.STATUS_VIOLATED.equals(request.getStatus())) {
            // 审核不通过（违规）
            content = "您举报的内容经审核确认违规，已被处理";
            type = Notification.TYPE_AUDIT_REJECT;
        } else {
            // 审核通过（无问题）
            content = "您举报的内容经审核确认无问题，已被忽略";
            type = Notification.TYPE_AUDIT_PASS;
        }

        notificationService.send(
                targetUserId,
                0L, // 系统发送
                type,
                report.getEntityType(),
                report.getEntityId(),
                content
        );
    }

    /**
     * 处罚被举报用户
     */
    private void punishReportedUser(Long adminId, Report report, ReportReviewRequest request) {
        Long targetUserId = getTargetUserId(report.getEntityType(), report.getEntityId());
        if (targetUserId == null) {
            return;
        }

        PunishRequest punishRequest = new PunishRequest();
        punishRequest.setUserId(targetUserId);
        punishRequest.setType(request.getPunishType());
        punishRequest.setReason(request.getPunishReason() != null ? request.getPunishReason()
                : "因举报违规内容被处罚");
        punishRequest.setDurationHours(request.getPunishDurationHours());

        try {
            punishmentService.punish(adminId, punishRequest);
        } catch (Exception e) {
            // 处罚失败不影响举报审核结果，仅记录日志
        }
    }

    /**
     * 隐藏被举报的内容
     */
    private void hideReportedContent(Integer entityType, Long entityId) {
        if (entityType == null || entityId == null) return;

        if (Report.ENTITY_TYPE_POST.equals(entityType)) {
            Post post = postMapper.selectById(entityId);
            if (post != null && !Integer.valueOf(Post.STATUS_DELETED).equals(post.getStatus())) {
                post.setStatus(Post.STATUS_HIDDEN);
                postMapper.updateById(post);
            }
        } else if (Report.ENTITY_TYPE_COMMENT.equals(entityType)) {
            // 评论没有隐藏状态，直接标记为已删除
            Comment comment = commentMapper.selectById(entityId);
            if (comment != null && !Integer.valueOf(Comment.STATUS_DELETED).equals(comment.getStatus())) {
                comment.setStatus(Comment.STATUS_DELETED);
                commentMapper.updateById(comment);
            }
        }
    }

    /**
     * 管理员：根据被举报实体清理所有相关举报记录
     */
    @Override
    public void clearReportsByEntity(Integer entityType, Long entityId) {
        if (entityType == null || entityId == null) {
            return;
        }
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(Report::getEntityType, entityType)
                .eq(Report::getEntityId, entityId);
        reportMapper.delete(wrapper);
    }

    /**
     * 清理同一被举报实体的其它举报记录，只保留当前这条
     */
    private void clearOtherReportsOfSameEntity(Report currentReport) {
        if (currentReport.getEntityType() == null || currentReport.getEntityId() == null) {
            return;
        }
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(Report::getEntityType, currentReport.getEntityType())
                .eq(Report::getEntityId, currentReport.getEntityId())
                .ne(Report::getId, currentReport.getId());
        reportMapper.delete(wrapper);
    }

    @Override
    public PageVO<Report> listPendingReports(int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);
        Page<Report> result = reportMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getStatus, Report.STATUS_PENDING)
                        .orderByAsc(Report::getCreatedAt)
        );
        return PageVO.of(result, result.getRecords());
    }

    @Override
    public PageVO<Report> listReports(Integer status, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .orderByDesc(Report::getCreatedAt);
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        Page<Report> result = reportMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageVO.of(result, result.getRecords());
    }

    @Override
    public PageVO<ReportVO> listReportsVO(Integer status, Integer entityType, Integer reason,
                                           Long reporterId, Long reportedUserId, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .orderByDesc(Report::getCreatedAt);

        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        if (entityType != null) {
            wrapper.eq(Report::getEntityType, entityType);
        }
        if (reason != null) {
            wrapper.eq(Report::getReason, reason);
        }
        if (reporterId != null) {
            wrapper.eq(Report::getReporterId, reporterId);
        }

        Page<Report> result = reportMapper.selectPage(new Page<>(page, pageSize), wrapper);

        // 转换为VO并过滤：
        // 1）过滤被举报内容已删除的记录（例如帖子已被删除）
        // 2）如果指定了 reportedUserId，仅保留对应用户的记录
        List<ReportVO> voList = new ArrayList<>();
        for (Report report : result.getRecords()) {
            // 跳过已删除内容的举报记录
            if (isEntityDeleted(report)) {
                continue;
            }

            ReportVO vo = convertToVO(report);
            // 如果指定了reportedUserId，进行过滤
            if (reportedUserId != null) {
                Long targetUserId = getTargetUserId(report.getEntityType(), report.getEntityId());
                if (!reportedUserId.equals(targetUserId)) {
                    continue;
                }
            }
            voList.add(vo);
        }

        Page<ReportVO> pageResult = new Page<>(page, pageSize);
        pageResult.setTotal(result.getTotal());
        pageResult.setRecords(voList);
        return PageVO.of(pageResult, voList);
    }

    @Override
    public PageVO<ReportVO> listMyReports(Long userId, Integer status, int page, int pageSize) {
        pageSize = Math.min(pageSize, 50);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, userId)
                .orderByDesc(Report::getCreatedAt);
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        Page<Report> result = reportMapper.selectPage(new Page<>(page, pageSize), wrapper);

        // 转换为VO
        List<ReportVO> voList = new ArrayList<>();
        for (Report report : result.getRecords()) {
            ReportVO vo = convertToVO(report);
            voList.add(vo);
        }
        return PageVO.of(result, voList);
    }

    /**
     * 判断被举报内容是否已经被删除
     */
    private boolean isEntityDeleted(Report report) {
        if (report.getEntityType() == null || report.getEntityId() == null) {
            return false;
        }
        if (Report.ENTITY_TYPE_POST.equals(report.getEntityType())) {
            Post post = postMapper.selectById(report.getEntityId());
            return post == null || Integer.valueOf(Post.STATUS_DELETED).equals(post.getStatus());
        } else if (Report.ENTITY_TYPE_COMMENT.equals(report.getEntityType())) {
            Comment comment = commentMapper.selectById(report.getEntityId());
            return comment == null || Integer.valueOf(Comment.STATUS_DELETED).equals(comment.getStatus());
        }
        return false;
    }

    private ReportVO convertToVO(Report report) {
        ReportVO vo = new ReportVO();
        vo.setId(report.getId());
        vo.setReporterId(report.getReporterId());
        vo.setEntityType(report.getEntityType());
        vo.setEntityTypeText(getEntityTypeText(report.getEntityType()));
        vo.setEntityId(report.getEntityId());
        vo.setReason(report.getReason());
        vo.setReasonText(getReasonText(report.getReason()));
        vo.setDetail(report.getDetail());
        vo.setStatus(report.getStatus());
        vo.setStatusText(getStatusText(report.getStatus()));
        vo.setReviewerId(report.getReviewerId());
        vo.setReviewNote(report.getReviewNote());
        vo.setReviewedAt(report.getReviewedAt());
        vo.setCreatedAt(report.getCreatedAt());

        // 填充举报人信息
        if (report.getReporterId() != null) {
            User reporter = userMapper.selectById(report.getReporterId());
            if (reporter != null) {
                vo.setReporterUsername(reporter.getUsername());
                vo.setReporterNickname(reporter.getNickname());
            }
        }

        // 填充审核人信息
        if (report.getReviewerId() != null) {
            User reviewer = userMapper.selectById(report.getReviewerId());
            if (reviewer != null) {
                vo.setReviewerNickname(reviewer.getNickname());
            }
        }

        // 填充被举报内容信息和被举报人信息
        Long targetUserId = null;
        if (report.getEntityType() != null && report.getEntityId() != null) {
            if (Report.ENTITY_TYPE_POST.equals(report.getEntityType())) {
                Post post = postMapper.selectById(report.getEntityId());
                if (post != null) {
                    vo.setEntityTitle(post.getTitle());
                    vo.setEntityAuthor(post.getUserId() != null ? getUserNickname(post.getUserId()) : null);
                    vo.setPostContent(post.getContent()); // 填充帖子正文
                    targetUserId = post.getUserId();
                }
            } else if (Report.ENTITY_TYPE_COMMENT.equals(report.getEntityType())) {
                Comment comment = commentMapper.selectById(report.getEntityId());
                if (comment != null) {
                    vo.setEntityTitle(comment.getContent() != null && comment.getContent().length() > 50
                            ? comment.getContent().substring(0, 50) + "..."
                            : comment.getContent());
                    vo.setEntityAuthor(comment.getUserId() != null ? getUserNickname(comment.getUserId()) : null);
                    targetUserId = comment.getUserId();
                }
            }
        }

        // 填充被举报人信息
        if (targetUserId != null) {
            User reportedUser = userMapper.selectById(targetUserId);
            if (reportedUser != null) {
                vo.setReportedUserId(reportedUser.getId());
                vo.setReportedUsername(reportedUser.getUsername());
                vo.setReportedNickname(reportedUser.getNickname());
            }
        }

        return vo;
    }

    private String getUserNickname(Long userId) {
        if (userId == null) return null;
        User user = userMapper.selectById(userId);
        return user != null ? user.getNickname() : null;
    }

    private String getEntityTypeText(Integer entityType) {
        if (entityType == null) return "未知";
        return switch (entityType) {
            case 1 -> "帖子";
            case 2 -> "评论";
            default -> "未知";
        };
    }

    private String getReasonText(Integer reason) {
        if (reason == null) return "未知";
        return switch (reason) {
            case 1 -> "垃圾广告";
            case 2 -> "违规内容";
            case 3 -> "侮辱漫骂";
            case 4 -> "其他";
            default -> "未知";
        };
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已处理-违规";
            case 2 -> "已处理-无问题";
            default -> "未知";
        };
    }
}
