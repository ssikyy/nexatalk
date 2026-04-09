package com.ttikss.nexatalk.service;

import com.ttikss.nexatalk.dto.SectionCreateRequest;
import com.ttikss.nexatalk.entity.Section;

import java.util.List;

/**
 * 分区模块业务层接口
 */
public interface SectionService {

    /** 查询所有正常状态的分区（按 sort_order 降序） */
    List<Section> listActiveSections();

    /** 查询所有分区（管理员用，包含禁用的） */
    List<Section> listAllSections();

    /** 根据 ID 查询分区详情 */
    Section getSectionById(Long id);

    /** 新增分区（管理员操作） */
    void createSection(SectionCreateRequest req);

    /** 更新分区（管理员操作） */
    void updateSection(Long id, SectionCreateRequest req);

    /** 禁用分区（管理员操作） */
    void disableSection(Long id);

    /** 启用分区（管理员操作） */
    void enableSection(Long id);
}
