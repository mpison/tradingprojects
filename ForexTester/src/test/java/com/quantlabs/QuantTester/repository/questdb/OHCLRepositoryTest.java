package com.quantlabs.QuantTester.repository.questdb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.quantlabs.QuantTester.model.questdb.OHCL;
import com.quantlabs.QuantTester.repository.questdb.OHCLRepository;
import com.kx.c;
import com.kx.c.KException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class OHCLRepositoryTest {

	@Autowired
	private OHCLRepository service;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	static String csvPath;

	String host;

	int port;

	static Properties props = new Properties();

	{
		try (InputStream input = new FileInputStream(
				"C:\\Users\\Melchedick Pison\\Documents\\ForexProject\\ForexTester\\ForexTester\\src\\main\\resources\\config.properties")) {
			props.load(input);

			host = props.getProperty("kdb.host", "localhost");
			port = Integer.parseInt(props.getProperty("kdb.port", "5000"));
			csvPath = props.getProperty("csv.path", "C:\\Users\\Melchedick Pison\\Downloads\\FBS_EURUSD1.csv");

			// conn = new c(host, port);
			System.out.println("Connected to kdb+ @ " + host + ":" + port);

		} catch (IOException e) {
			System.err.println("Failed to load config.properties");
		}
	}

	// Test
	public void testIntegrationCRUD() throws Exception {
		// LocalDateTime timestamp = LocalDateTime.of(2024, 5, 1, 13, 45, 30);

		OHCL data = new OHCL("EURUSD", "M1", "FBS", 1.1f, 1.2f, 1.0f, 1.15f, 1000L, "2024.05.01 13:45");

		// Insert
		service.insertBulk("FBS", "M1", "EURUSD", List.of(data));
		// Thread.sleep(500); // allow time for ingestion

		// Verify read
		List<OHCL> results = service.getBySymbol("FBS", "M1", "EURUSD");
		assertFalse(results.isEmpty());

		// Update
		OHCL updated = new OHCL("EURUSD", "M1", "OANDA", 1.11f, 1.21f, 1.01f, 1.16f, 2000L, "2024.05.01 13:45");
		int updatedRows = service.update(updated);
		assertTrue(updatedRows >= 0);

		// Delete
		// int deletedRows = service.delete("EURUSD", "M1", "OANDA",
		// "2024-05-01T13:45:30");
		// assertTrue(deletedRows >= 0);
	}

	//Test
	public void bulkUpsertToQuestDB() throws Exception {

		BufferedReader reader = new BufferedReader(new FileReader(csvPath));
		String line;

		int i = 0;

		int BATCH_SIZE = 100000;

		List<OHCL> chartForexDataList = new ArrayList<OHCL>();

		String broker = "FBS";
		String symbol = "EURUSD";
		String tf = "M1";
		
		if (service.createOHCLTableBy(broker, tf, symbol)) {

			while ((line = reader.readLine()) != null) {
				if (line.startsWith("timestamp"))
					continue;
				String[] fields = line.split(",");
				String date = fields[0];
				String time = fields[1];
				Float open = Float.valueOf(fields[2]);
				Float high = Float.valueOf(fields[3]);
				Float low = Float.valueOf(fields[4]);
				Float close = Float.valueOf(fields[5]);
				Long volume = Long.valueOf(fields[6]);

				String ts = date + " " + time;// TimeSeriesConverter.convertToKdbTimestamp(date + " " + time);

				System.out.println(ts);

				OHCL data = new OHCL(symbol, tf, broker, open, high, low, close, volume, ts);

				chartForexDataList.add(data);

				// Insert batch every 100k
				if ((i + 1) % BATCH_SIZE == 0) {
					// kdbService.insertBatch(symbolList, tfList, brokerList, openList, highList,
					// lowList, closeList, volumeList, tsList);

					service.insertBulk(broker, tf, symbol, chartForexDataList);

					chartForexDataList.clear();

					System.out.println("Inserted batch: " + (i + 1));
				}

				i++;
			}

			if (chartForexDataList.size() > 0) {

				service.insertBulk(broker, tf, symbol, chartForexDataList);
			}

			assertTrue(i >= 0);
			
			
			service.convertM1ToOtherTimeFramesBy(broker, tf, symbol);
		}
	}
	
	
	//Test
	public void convertM1ToOtherTF() throws Exception {
		String broker = "FBS";
		String symbol = "EURUSD";
		String tf = "M1";
		service.convertM1ToOtherTimeFramesBy(broker, tf, symbol);
	}

}
