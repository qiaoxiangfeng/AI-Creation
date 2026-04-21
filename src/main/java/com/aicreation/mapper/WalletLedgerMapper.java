package com.aicreation.mapper;

import com.aicreation.entity.po.WalletLedger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WalletLedgerMapper {

    WalletLedger selectByPrimaryKey(@Param("id") Long id);

    WalletLedger selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    int insert(WalletLedger ledger);

    List<WalletLedger> selectByUserId(@Param("userId") Long userId);
}

