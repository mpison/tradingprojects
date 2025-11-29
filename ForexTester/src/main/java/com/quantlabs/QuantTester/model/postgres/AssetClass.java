package com.quantlabs.QuantTester.model.postgres;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "asset_classes")
public class AssetClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "assetClass")
    private List<Symbol> symbols;
}