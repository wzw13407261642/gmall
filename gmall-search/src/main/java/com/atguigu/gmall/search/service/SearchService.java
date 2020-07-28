package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParam;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    RestHighLevelClient client;

    public SearchResponseVo search(SearchParam searchParam) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("goods");
        SearchSourceBuilder searchSourceBuilder = buildDsl(searchParam);
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchResponseVo searchResponseVo = parseResult(search);
            searchResponseVo.setPageNum(searchParam.getPageNum());
            searchResponseVo.setPageSize(searchParam.getPageSize());
            return searchResponseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SearchResponseVo parseResult(SearchResponse searchResponse){
        SearchResponseVo responseVo=new SearchResponseVo();
        SearchHits hits = searchResponse.getHits();
        responseVo.setTotal(hits.getTotalHits());
        SearchHit[] hits1 = hits.getHits();
        List<Goods> goodsList = Arrays.stream(hits1).map(hit -> {
            String sourceAsString = hit.getSourceAsString();
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField title = highlightFields.get("title");
            String newTitle = title.getFragments()[0].toString();
            goods.setTitle(newTitle);
            return goods;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        ParsedLongTerms brandIdAgg =(ParsedLongTerms) aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)){
            List<BrandEntity> collect = buckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                brandEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                Map<String, Aggregation> brandIdAggParent = ((Terms.Bucket) bucket).getAggregations().asMap();
                ParsedStringTerms brandNameAgg =(ParsedStringTerms) brandIdAggParent.get("brandNameAgg");
                brandEntity.setName(brandNameAgg.getBuckets().get(0).getKeyAsString());
                ParsedStringTerms brandLogAgg =(ParsedStringTerms) brandIdAggParent.get("brandLogAgg");
                brandEntity.setLogo(brandLogAgg.getBuckets().get(0).getKeyAsString());
                return brandEntity;
            }).collect(Collectors.toList());
            responseVo.setBrands(collect);
        }

        ParsedLongTerms categoryIdAgg =(ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryIdAggBuckets)){
            List<CategoryEntity> categoryEntities = categoryIdAggBuckets.stream().map(categoryBucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                categoryEntity.setId(((Terms.Bucket) categoryBucket).getKeyAsNumber().longValue());
                ParsedStringTerms categoryNameAgg =(ParsedStringTerms) ((Terms.Bucket) categoryBucket).getAggregations().asMap().get("categoryNameAgg");
                String categoryName = categoryNameAgg.getBuckets().get(0).getKeyAsString();
                categoryEntity.setName(categoryName);
                return categoryEntity;
            }).collect(Collectors.toList());
            responseVo.setCategories(categoryEntities);
        }

        ParsedNested attrAgg =(ParsedNested) aggregationMap.get("attrAgg");
        Map<String, Aggregation> aggregationMap1 = attrAgg.getAggregations().asMap();
        ParsedLongTerms attrIdAgg =(ParsedLongTerms) aggregationMap1.get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(attrIdAggBuckets)){
            List<SearchResponseAttrVo> collect = attrIdAggBuckets.stream().map(attrIdAgg2 -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                Long attrId = ((Terms.Bucket) attrIdAgg2).getKeyAsNumber().longValue();
                searchResponseAttrVo.setAttrId(attrId);
                Map<String, Aggregation> aggregationMap2 = ((Terms.Bucket) attrIdAgg2).getAggregations().asMap();
                ParsedStringTerms attrNameAgg =(ParsedStringTerms) aggregationMap2.get("attrNameAgg");
                searchResponseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                ParsedStringTerms attrValueAgg = (ParsedStringTerms)aggregationMap2.get("attrValueAgg");
                List<? extends Terms.Bucket> buckets1 = attrValueAgg.getBuckets();
                List<String> collect1 = buckets1.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                searchResponseAttrVo.setAttrValues(collect1);
                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            responseVo.setFilters(collect);
        }
        return responseVo;
    }

    public SearchSourceBuilder buildDsl(SearchParam searchParam) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        String keyword = searchParam.getKeyword();
        if (StringUtils.isEmpty(keyword)) {
            return null;
        }
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));
        List<Long> brandId = searchParam.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }
        Long cid = searchParam.getCid();
        if (cid != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("categoryId", cid));
        }
        Double priceFrom = searchParam.getPriceFrom();
        Double priceTo = searchParam.getPriceTo();
        if (priceFrom != null || priceTo != null) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
            if (priceFrom != null) {
                rangeQueryBuilder.gte(priceFrom);
            }
            if (priceTo != null) {
                rangeQueryBuilder.lte(priceTo);
            }
            boolQueryBuilder.filter(rangeQueryBuilder);
        }
        Boolean store = searchParam.getStore();
        if (store != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
        }


        // 1.2.5. 规格参数的过滤 props=5:高通-麒麟&props=6:骁龙865-硅谷1000
        List<String> props = searchParam.getProps();
        if (!CollectionUtils.isEmpty(props)) {
            props.forEach(prop -> {
                String[] attrs = StringUtils.split(prop, ":");
                if (attrs != null && attrs.length == 2) {
                    String attrId = attrs[0];
                    String attrValue = attrs[1];
                    String[] atrValues = StringUtils.split(attrValue, "-");
                    if (atrValues==null){
                      //  System.out.println("我来了");
                     //   System.out.println("33333"+attrValue);
                        atrValues=new String[1];
                        atrValues[0]=attrValue;
                       // System.out.println("44444"+atrValues[0]);
                    }
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                   // System.out.println("1111111111111"+attrId);
                  //  System.out.println("2222222222222"+ Arrays.toString(atrValues));
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attrId));
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", atrValues));
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));
                }
            });
        }
        sourceBuilder.query(boolQueryBuilder);

        Integer sort = searchParam.getSort();
        String field = "";
        SortOrder sortOrder = null;
        switch (sort) {
            case 1:
                field = "price";
                sortOrder = SortOrder.ASC;
                break;
            case 2:
                field = "price";
                sortOrder = SortOrder.DESC;
                break;
            case 3:
                field = "createTime";
                sortOrder = SortOrder.DESC;
                break;
            case 4:
                field = "price";
                sortOrder = SortOrder.DESC;
                break;
            default:
                field = "_score";
                sortOrder = SortOrder.DESC;
                break;
        }
        sourceBuilder.sort(field, sortOrder);

        Integer pageNum = searchParam.getPageNum();
        Integer pageSize = searchParam.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);

        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<font style='color:red'>").postTags("</font>"));

        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId").
                subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName")).
                subAggregation(AggregationBuilders.terms("brandLogAgg").field("logo")));

        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId").
                subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));

        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs").
                subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));

        sourceBuilder.fetchSource(new String[]{"skuId","title","price","subTitle","defaultImage"},null );
        System.out.println(sourceBuilder.toString());
        return sourceBuilder;
    }
}
