package com.quantlabs.QuantTester.model.postgres;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "symbols", uniqueConstraints = @UniqueConstraint(columnNames = {"symbol_code", "broker_id"}))
public class Symbol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_id")
    private Broker broker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_class_id")
    private AssetClass assetClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_type_id")
    private InstrumentType instrumentType;

    @Column(name = "symbol_code")
    private String symbolCode;

    private String displayName;
    private String baseCurrency;
    private String quoteCurrency;
    private String exchangeName;

    @OneToOne(mappedBy = "symbol", cascade = CascadeType.ALL)
    private ContractSpecification contractSpecification;

    @OneToMany(mappedBy = "symbol", cascade = CascadeType.ALL)
    private List<TradingSession> tradingSessions;
}
