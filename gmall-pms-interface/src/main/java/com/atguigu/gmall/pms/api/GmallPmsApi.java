package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface GmallPmsApi {

    @PostMapping("pms/spu/page")
    @ApiOperation("分页查询")
    public ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);


    @GetMapping("pms/sku/spu/{SpuId}")
    public ResponseVo<List<SkuEntity>> getListSkuBySpuId(@PathVariable("SpuId") Long SpuId);

    @GetMapping("pms/brand/{id}")
    @ApiOperation("详情查询")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);


    @GetMapping("pms/category/{id}")
    @ApiOperation("详情查询")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    @ApiOperation("根据spuID查询对应的检索属性及值")
    @GetMapping("pms/spuattrvalue/spu/{supId}")
    public ResponseVo<List<SpuAttrValueEntity>> getAttrAndValueBySpuId(@PathVariable("supId") Long supId);

    @ApiOperation("根据skuId查询对应的检索属性和值")
    @GetMapping("pms/skuattrvalue/sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> getAttrAndValueBySkuId(@PathVariable("skuId") Long skuId);

}
