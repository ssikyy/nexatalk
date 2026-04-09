package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.dto.SectionCreateRequest;
import com.ttikss.nexatalk.entity.Section;
import com.ttikss.nexatalk.exception.BusinessException;
import com.ttikss.nexatalk.mapper.SectionMapper;
import com.ttikss.nexatalk.service.SectionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分区模块业务实现
 */
@Service
public class SectionServiceImpl implements SectionService {

    private final SectionMapper sectionMapper;

    public SectionServiceImpl(SectionMapper sectionMapper) {
        this.sectionMapper = sectionMapper;
    }

    @Override
    public List<Section> listActiveSections() {
        return sectionMapper.selectList(
                new LambdaQueryWrapper<Section>()
                        .eq(Section::getStatus, Section.STATUS_NORMAL)
                        .orderByDesc(Section::getSortOrder)
        );
    }

    @Override
    public List<Section> listAllSections() {
        return sectionMapper.selectList(
                new LambdaQueryWrapper<Section>()
                        .orderByDesc(Section::getSortOrder)
        );
    }

    @Override
    public Section getSectionById(Long id) {
        Section section = sectionMapper.selectById(id);
        if (section == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.code(), "分区不存在");
        }
        return section;
    }

    @Override
    public void createSection(SectionCreateRequest req) {
        // 检查分区名是否重复
        Long count = sectionMapper.selectCount(
                new LambdaQueryWrapper<Section>().eq(Section::getName, req.getName())
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "分区名称已存在");
        }

        Section section = new Section();
        section.setName(req.getName());
        section.setDescription(req.getDescription() != null ? req.getDescription() : "");
        section.setIconUrl(req.getIconUrl() != null ? req.getIconUrl() : "");
        section.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        section.setStatus(Section.STATUS_NORMAL);
        sectionMapper.insert(section);
    }

    @Override
    public void updateSection(Long id, SectionCreateRequest req) {
        Section existing = getSectionById(id);

        // 如果改了名字，检查新名字是否与其他分区重复
        if (!existing.getName().equals(req.getName())) {
            Long count = sectionMapper.selectCount(
                    new LambdaQueryWrapper<Section>().eq(Section::getName, req.getName())
            );
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "分区名称已存在");
            }
        }

        LambdaUpdateWrapper<Section> wrapper = new LambdaUpdateWrapper<Section>()
                .eq(Section::getId, id)
                .set(Section::getName, req.getName());

        if (req.getDescription() != null) {
            wrapper.set(Section::getDescription, req.getDescription());
        }
        if (req.getIconUrl() != null) {
            wrapper.set(Section::getIconUrl, req.getIconUrl());
        }
        if (req.getSortOrder() != null) {
            wrapper.set(Section::getSortOrder, req.getSortOrder());
        }

        sectionMapper.update(null, wrapper);
    }

    @Override
    public void disableSection(Long id) {
        getSectionById(id); // 确保存在
        sectionMapper.update(null, new LambdaUpdateWrapper<Section>()
                .eq(Section::getId, id)
                .set(Section::getStatus, Section.STATUS_DISABLED)
        );
    }

    @Override
    public void enableSection(Long id) {
        getSectionById(id); // 确保存在
        sectionMapper.update(null, new LambdaUpdateWrapper<Section>()
                .eq(Section::getId, id)
                .set(Section::getStatus, Section.STATUS_NORMAL)
        );
    }
}
