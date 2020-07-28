package com.atguigu.gmall.search.pojo;


import lombok.Data;

import java.util.List;

@Data
public class SearchParam {

    private String keyword;

    private List<Long> brandId;

    private Long cid;

    private List<String> props;

    private Integer sort = 0;

    private Double priceFrom;
    private Double priceTo;

    private Integer pageNum=1;

    private final Integer pageSize = 20;

    private Boolean store;

}
