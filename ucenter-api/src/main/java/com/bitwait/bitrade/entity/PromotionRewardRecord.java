package com.bitwait.bitrade.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ragan QQ:2098401701 E-mail:bitwait@qq.com
 * @date 2020年03月20日
 */
@Data
@Builder
public class PromotionRewardRecord {
    private String symbol;
    private String remark;
    private BigDecimal amount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

}
