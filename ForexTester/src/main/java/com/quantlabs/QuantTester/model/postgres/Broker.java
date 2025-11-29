package com.quantlabs.QuantTester.model.postgres;

import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "brokers")
public class Broker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String website;
    private String regulationInfo;
    private String apiEndpoint;

    @OneToMany(mappedBy = "broker")
    private List<Symbol> symbols;
}