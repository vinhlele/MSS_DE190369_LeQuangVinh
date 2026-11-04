package com.mss301.brand.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// [TODO-5.5: DTO BrandResponse kèm watchCount]
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponse {
    private Integer brandId;
    private String brandName;
    private String countryOfOrigin;
    private long watchCount;
}
