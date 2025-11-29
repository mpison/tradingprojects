package com.quantlabs.QuantTester.repository.postgres;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quantlabs.QuantTester.model.postgres.Broker;

@Repository
public interface BrokerRepository extends JpaRepository<Broker, Long> {
    boolean existsByName(String name);
}