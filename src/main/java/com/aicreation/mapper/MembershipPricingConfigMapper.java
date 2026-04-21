package com.aicreation.mapper;

import com.aicreation.entity.po.MembershipPricingConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MembershipPricingConfigMapper {

    MembershipPricingConfig selectByPrimaryKey(@Param("id") Long id);

    List<MembershipPricingConfig> selectEnabledByTier(@Param("tier") String tier);

    int insert(MembershipPricingConfig row);

    int updateByPrimaryKey(MembershipPricingConfig row);

    List<MembershipPricingConfig> selectAllForAdmin();
}
