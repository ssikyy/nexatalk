package com.ttikss.nexatalk.service;

import com.ttikss.nexatalk.dto.ReportCreateRequest;
import com.ttikss.nexatalk.dto.ReportReviewRequest;
import com.ttikss.nexatalk.entity.Report;
import com.ttikss.nexatalk.vo.PageVO;
import com.ttikss.nexatalk.vo.ReportVO;

/**
 * 举报模块业务层接口
 */
public interface ReportService {

    /** 提交举报 */
    void createReport(Long reporterId, ReportCreateRequest request);
    
    /** 管理员审核举报 */
    void reviewReport(Long adminId, Long reportId, ReportReviewRequest request);

    /** 管理员查询举报列表（返回VO，带举报详情） */
    PageVO<ReportVO> listReportsVO(Integer status, Integer entityType, Integer reason,
                                    Long reporterId, Long reportedUserId, int page, int pageSize);

    /** 管理员查询待处理的举报列表 */
    PageVO<Report> listPendingReports(int page, int pageSize);

    /** 管理员查询举报列表（返回实体） */
    PageVO<Report> listReports(Integer status, int page, int pageSize);

    /** 用户查询自己的举报记录 */
    PageVO<ReportVO> listMyReports(Long userId, Integer status, int page, int pageSize);

    /** 管理员：根据被举报实体清理所有相关举报记录 */
    void clearReportsByEntity(Integer entityType, Long entityId);
}
