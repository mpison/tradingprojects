package com.quantlabs.QuantTester.repository.postgres;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quantlabs.QuantTester.model.postgres.InstrumentType;


@Repository
public interface InstrumentTypeRepository extends JpaRepository<InstrumentType, Long> {}
