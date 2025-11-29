package com.quantlabs.QuantTester.repository.postgres;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quantlabs.QuantTester.model.postgres.Symbol;

@Repository
public interface SymbolRepository extends JpaRepository<Symbol, Long> {
    Optional<Symbol> findBySymbolCode(String symbolCode);
    Optional<Symbol> findBySymbolCodeAndBrokerName(String symbolCode, String brokerName);
}