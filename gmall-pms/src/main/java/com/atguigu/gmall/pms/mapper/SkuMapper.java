package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.SkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * sku信息
 * 
 * @author wzw
 * @email wzw13407261642@126.com
 * @date 2020-07-19 10:43:20
 */
@Mapper
@Repository
public interface SkuMapper extends BaseMapper<SkuEntity> {
	
}
