package com.quantlabs.QuantTester.repository.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.quantlabs.QuantTester.model.postgres.Symbol;

@SpringBootTest(properties = "spring.profiles.active=test")
public class DataServiceTest {
	@Autowired
	BrokerRepository brokerRepository;
	@Autowired
	SymbolRepository symbolRepository;
	//@Autowired
	//TraderRepository traderRepository;

	@Test
	void verifyBrokerInserted() {
		assertTrue(brokerRepository.existsByName("FBS"));
	}

	@Test
	void regressionCheckInsertedSymbolsAndTraders() {
		List<Symbol> symbols = symbolRepository.findAll();
		//List<Trader> traders = traderRepository.findAll();
		
		//Symbol.builder().

		assertEquals(1, symbols.size(), "Should have inserted 1 symbols");
		//assertTrue(symbols.stream().anyMatch(s -> s.getSymbol().equals("EURUSD")));
		//assertTrue(symbols.stream().anyMatch(s -> s.getSymbol().equals("AAPL")));

		//assertEquals(2, traders.size(), "Should have inserted 2 traders");
		//assertTrue(traders.stream().anyMatch(t -> t.getUsername().equals("jdoe")));
		//assertTrue(traders.stream().anyMatch(t -> t.getUsername().equals("msmith")));
	}
}
