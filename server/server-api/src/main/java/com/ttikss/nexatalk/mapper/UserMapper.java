package com.ttikss.nexatalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttikss.nexatalk.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层（UserMapper）
 *
 * 这个类做什么：
 * - 负责与 user 表交互（增删改查）
 * - 继承 BaseMapper 后自带 insert/select/update/delete 等方法
 * - 目前不写任何 XML/SQL 就能先跑通基本 CRUD
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}