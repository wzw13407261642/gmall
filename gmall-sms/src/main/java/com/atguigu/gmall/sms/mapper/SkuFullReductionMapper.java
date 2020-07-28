package com.atguigu.gmall.sms.mapper;

import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 商品满减信息
 * 
 * @author wzw
 * @email wzw13407261642@126.com
 * @date 2020-07-19 11:09:16
 */
@Mapper
@Repository
public interface SkuFullReductionMapper extends BaseMapper<SkuFullReductionEntity> {
	
}
