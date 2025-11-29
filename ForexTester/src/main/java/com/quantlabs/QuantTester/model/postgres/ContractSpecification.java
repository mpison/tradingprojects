package com.quantlabs.QuantTester.model.postgres;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "contract_specifications")
public class ContractSpecification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id")
    private Symbol symbol;

    private BigDecimal contractSize;
    private BigDecimal minLotSize;
    private BigDecimal maxLotSize;
    private BigDecimal lotStep;
    private BigDecimal tickSize;
    private BigDecimal tickValue;
    private BigDecimal leverage;
    private String marginCurrency;
    private BigDecimal swapLong;
    private BigDecimal swapShort;
    private BigDecimal commissionPerLot;
    private Boolean isCommissionPercent;
    private Integer tripleSwapDay;
}