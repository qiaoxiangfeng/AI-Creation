package com.aicreation.mapper;

import com.aicreation.entity.po.UserWallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserWalletMapper {

    UserWallet selectByPrimaryKey(@Param("id") Long id);

    UserWallet selectByUserId(@Param("userId") Long userId);

    /**
     * 行级锁查询（用于扣款/冻结并发安全）
     */
    UserWallet selectByUserIdForUpdate(@Param("userId") Long userId);

    int insert(UserWallet wallet);

    int updateByPrimaryKey(UserWallet wallet);
}

