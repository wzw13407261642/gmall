package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuEntity;
import lombok.Data;

import java.util.List;

@Data
public class SpuVo extends SpuEntity {

    List<SpuAttrValueVo> baseAttrs;

    List<SkuVo> skus;

    List<String> spuImages;

}
