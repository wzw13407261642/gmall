package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author wzw
 * @email wzw13407261642@126.com
 * @date 2020-07-19 11:13:23
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
