package com.quantlabs.QuantTester.model.postgres;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "instrument_types")
public class InstrumentType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "instrumentType")
    private List<Symbol> symbols;
}