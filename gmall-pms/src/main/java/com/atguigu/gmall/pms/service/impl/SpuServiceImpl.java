package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVO;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo getSpuPageList(Long categoryId, PageParamVo pageParamVo) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        if (categoryId != 0) {
            wrapper.eq("category_id", categoryId);
        }
        String key = pageParamVo.getKey();
        if (StringUtils.isNotBlank(key)) {
            wrapper.and((w) -> {
                w.like("id", key).or().like("name", key);
            });
        }

        IPage<SpuEntity> page = this.page(
                pageParamVo.getPage(),
                wrapper
        );

        return new PageResultVo(page);
    }

    @Autowired
    SpuDescMapper spuDescMapper;

    @Autowired
    SpuAttrValueService spuAttrValueService;

    @Autowired
    SkuMapper skuMapper;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuAttrValueService skuAttrValueService;

    @Autowired
    GmallSmsClient gmallSmsClient;

    @Autowired
    SpuDescService spuDescService;

    @Override
    //@Transactional(propagation = Propagation.REQUIRED)
    @GlobalTransactional
    public void Bigsave(SpuVo spuVo) {
        // spu相关信息
        Long spuId = saveSpu(spuVo);
        spuDescService.saveSpuDesc(spuVo, spuId);
        saveBaseAttr(spuVo, spuId);
        saveSku(spuVo, spuId);
        //int i = 1 / 0;

    }

    private void saveSku(SpuVo spuVo, Long spuId) {
        // sku相关信息
        List<SkuVo> skus = spuVo.getSkus();
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }

        skus.forEach(sku -> {
            SkuEntity skuEntity = new SkuEntity();
            BeanUtils.copyProperties(sku, skuEntity);
            skuEntity.setSpuId(spuId);
            skuEntity.setCatagoryId(spuVo.getCategoryId());
            skuEntity.setBrandId(spuVo.getBrandId());
            List<String> images = sku.getImages();
            if (!CollectionUtils.isEmpty(images)) {
                //skuEntity.setDefaultImage(StringUtils.isBlank(sku.getDefaultImage()) ? sku.getDefaultImage() : images.get(0));
                skuEntity.setDefaultImage(sku.getDefaultImage() == null ? images.get(0) : sku.getDefaultImage());
            }
            skuMapper.insert(skuEntity);
            Long skuId = skuEntity.getId();

            // 保存sku图片信息
            if (!CollectionUtils.isEmpty(images)) {
                List<SkuImagesEntity> collect = images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setSort(0);
                    skuImagesEntity.setUrl(image);
                    //System.out.println(image.equals(skuEntity.getDefaultImage()) ? 1 : 0);
                    skuImagesEntity.setDefaultStatus(image.equals(skuEntity.getDefaultImage()) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList());
                skuImagesService.saveBatch(collect);
            }

            // 保存销售规格属性
            List<SkuAttrValueEntity> saleAttrs = sku.getSaleAttrs();
            saleAttrs.forEach(skuattr -> {
                skuattr.setSort(0);
                skuattr.setSkuId(skuId);
            });
            skuAttrValueService.saveBatch(saleAttrs);

            SkuSaleVO skuSaleVO = new SkuSaleVO();
            BeanUtils.copyProperties(sku, skuSaleVO);
            skuSaleVO.setSkuId(skuId);
            gmallSmsClient.saveSkuSale(skuSaleVO);
        });
    }

    private void saveBaseAttr(SpuVo spuVo, Long spuId) {
        List<SpuAttrValueVo> baseAttrs = spuVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            List<SpuAttrValueEntity> collect = baseAttrs.stream().map(SpuAttrValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(SpuAttrValueVo, spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                spuAttrValueEntity.setSort(0);
                return spuAttrValueEntity;
            }).collect(Collectors.toList());
            spuAttrValueService.saveBatch(collect);
        }
    }


    private Long saveSpu(SpuVo spuVo) {
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());
        this.save(spuVo);

        return spuVo.getId();
    }

}