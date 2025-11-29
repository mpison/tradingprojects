package com.quantlabs.QuantTester.service.kdb;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// not use 
public class KdbForexQuery {

	private String host;
	private int port;
	private String username;
	private String password;

	public KdbForexQuery(String host, int port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	public KdbForexQuery(String host, int port) {
		this(host, port, null, null);
	}

	public List<Map<String, Object>> queryForexData(String query) throws IOException {
		try (Socket socket = new Socket(host, port)) {
			socket.setTcpNoDelay(true);
			return executeForexQuery(socket, query);
		}
	}

	private List<Map<String, Object>> executeForexQuery(Socket socket, String query) throws IOException {
		byte[] qBytes = query.getBytes("UTF-8");
		int msgLen = 1 + (username != null ? username.length() + 1 : 0) + 1 + qBytes.length;
		ByteBuffer buffer = ByteBuffer.allocate(4 + msgLen).order(ByteOrder.LITTLE_ENDIAN);

		buffer.put((byte) -1); // Message type: -1 for sync query

		if (username != null) {
			buffer.put(username.getBytes("US-ASCII"));
			buffer.put((byte) 0);
		}
		buffer.put((byte) 0);

		buffer.put(qBytes);
		buffer.putInt(0, msgLen);

		socket.getOutputStream().write(buffer.array());

		return readKdbForexResponse(socket.getInputStream());
	}

	private List<Map<String, Object>> readKdbForexResponse(java.io.InputStream inputStream) throws IOException {
		ByteBuffer lengthBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		if (inputStream.read(lengthBuffer.array()) != 4) {
			throw new IOException("Failed to read response length");
		}
		int responseLength = lengthBuffer.getInt(0);

		ByteBuffer responseBuffer = ByteBuffer.allocate(responseLength).order(ByteOrder.LITTLE_ENDIAN);
		if (inputStream.read(responseBuffer.array()) != responseLength) {
			throw new IOException("Failed to read full response");
		}

		byte responseType = responseBuffer.get();
		switch (responseType) {
		case 1: // Error
			byte[] errorBytes = new byte[responseBuffer.remaining()];
			responseBuffer.get(errorBytes);
			throw new IOException("KDB+ Error: " + new String(errorBytes, "UTF-8"));
		case 98: // Table
			return deserializeTable(responseBuffer);
		default:
			throw new IOException("Unexpected KDB+ response type for table: " + responseType);
		}
	}

	private List<Map<String, Object>> deserializeTable(ByteBuffer buffer) throws IOException {
		// Skip the table attribute (usually 0)
		buffer.getInt();

		// Deserialize the dictionary (column names -> column data)
		Map<String, Object> dictionary = deserializeDictionary(buffer);
		List<Map<String, Object>> result = new ArrayList<>();

		if (dictionary.isEmpty()) {
			return result; // Empty table
		}

		// Assuming all column lists have the same size
		String firstColumnName = dictionary.keySet().iterator().next();
		int rowCount = getListSize(dictionary.get(firstColumnName));

		for (int i = 0; i < rowCount; i++) {
			Map<String, Object> row = new HashMap<>();
			for (Map.Entry<String, Object> entry : dictionary.entrySet()) {
				String columnName = entry.getKey();
				Object columnData = entry.getValue();
				if (columnData instanceof List) {
					row.put(columnName, ((List<?>) columnData).get(i));
				} else if (columnData != null && rowCount == 1) {
					// Handle single-row table where columns might be single values
					row.put(columnName, columnData);
				} else {
					throw new IOException("Inconsistent column data structure for column: " + columnName);
				}
			}
			result.add(row);
		}
		return result;
	}

	private Map<String, Object> deserializeDictionary(ByteBuffer buffer) throws IOException {
		Map<String, Object> dictionary = new HashMap<>();

		// Deserialize keys (symbols)
		List<String> keys = deserializeSymbolList(buffer);

		// Deserialize values (lists of corresponding types)
		byte valueType = buffer.get();
		int listCount = buffer.getInt(); // Should match the number of keys

		if (keys.size() != listCount) {
			throw new IOException("Mismatched number of keys and value lists in dictionary");
		}

		for (int i = 0; i < listCount; i++) {
			ByteBuffer valueBuffer = ByteBuffer.wrap(buffer.array(), buffer.position(), buffer.remaining())
					.order(ByteOrder.LITTLE_ENDIAN);
			Object valueList = deserializeListByType(valueType, valueBuffer);
			dictionary.put(keys.get(i), valueList);
			buffer.position(buffer.position() + getListSizeInBytesByType(valueType, valueBuffer));
		}

		return dictionary;
	}

	private List<String> deserializeSymbolList(ByteBuffer buffer) throws IOException {
		byte type = buffer.get();
		if (type != 100) { // Type 100 is a list of symbols
			throw new IOException("Expected symbol list (type 100), but got: " + type);
		}
		int count = buffer.getInt();
		List<String> symbols = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			byte symbolType = buffer.get();
			if (symbolType != 11) {
				throw new IOException("Expected symbol (type 11) within symbol list, but got: " + symbolType);
			}
			int len = buffer.getInt();
			byte[] symbolBytes = new byte[len];
			buffer.get(symbolBytes);
			symbols.add(new String(symbolBytes, "UTF-8"));
		}
		return symbols;
	}

	private Object deserializeListByType(byte listType, ByteBuffer buffer) throws IOException {
		int count = buffer.getInt();
		switch (Math.abs(listType)) {
		case 1: // Boolean
			List<Boolean> booleans = new ArrayList<>(count);
			for (int i = 0; i < count; i++)
				booleans.add(buffer.get() != 0);
			return booleans;
		case 4: // Integer
			List<Integer> integers = new ArrayList<>(count);
			for (int i = 0; i < count; i++)
				integers.add(buffer.getInt());
			return integers;
		case 5: // Long
			List<Long> longs = new ArrayList<>(count);
			for (int i = 0; i < count; i++)
				longs.add(buffer.getLong());
			return longs;
		case 6: // Float
			List<Float> floats = new ArrayList<>(count);
			for (int i = 0; i < count; i++)
				floats.add(buffer.getFloat());
			return floats;
		case 9: // Real (Double)
			List<Double> doubles = new ArrayList<>(count);
			for (int i = 0; i < count; i++)
				doubles.add(buffer.getDouble());
			return doubles;
		case 10: // String (list of chars)
			List<String> strings = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				int len = buffer.getInt();
				byte[] chars = new byte[len];
				buffer.get(chars);
				strings.add(new String(chars, "UTF-8"));
			}
			return strings;
		case 11: // Symbol (list of symbols) - handled by deserializeSymbolList
			throw new IOException("Unexpected symbol list type within general list");
		case 12: // Timestamp
			List<Date> timestamps = new ArrayList<>(count);
			for (int i = 0; i < count; i++)
				timestamps.add(new Date(buffer.getLong() / 1_000_000L)); // Convert ns to ms
			return timestamps;
		// Add other types as needed
		default:
			throw new IOException("Unsupported list element type: " + listType);
		}
	}

	private int getListSize(Object list) {
		if (list instanceof List) {
			return ((List<?>) list).size();
		}
		return 1; // Assuming single value represents a single-row column
	}

	private int getListSizeInBytesByType(byte listType, ByteBuffer buffer) {
		int count = buffer.getInt(buffer.position()); // Peek at the count
		int baseSize = 4; // For the count itself
		switch (Math.abs(listType)) {
		case 1:
			return baseSize + count * 1;
		case 4:
			return baseSize + count * 4;
		case 5:
			return baseSize + count * 8;
		case 6:
			return baseSize + count * 4;
		case 9:
			return baseSize + count * 8;
		case 10: // List of strings - need to sum string lengths + 4 bytes per length
			int totalSize = baseSize;
			int currentPosition = buffer.position() + 4; // Skip the count
			for (int i = 0; i < count; i++) {
				int len = ByteBuffer.wrap(buffer.array(), currentPosition, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
				totalSize += 4 + len;
				currentPosition += 4 + len;
			}
			return totalSize;
		case 11:
			return baseSize + count * 5; // Assuming 4-byte length + 1-byte type for each symbol
		case 12:
			return baseSize + count * 8;
		default:
			return baseSize; // Unknown size
		}
	}

	public static void main(String[] args) {
		String host = "localhost";
		int port = 5000;
		KdbForexQuery kdb = new KdbForexQuery(host, port);

		try {
			String q = "chartForexDataHistory";
			List<Map<String, Object>> results = kdb.queryForexData(q);
			System.out.println("Query Result for '" + q + "':");
			for (Map<String, Object> row : results) {
				System.out.println(row);
			}

			String filteredQ = "select from chartForexDataHistory where symbol = `EURUSD, tf = `M1";
			List<Map<String, Object>> filteredResults = kdb.queryForexData(filteredQ);
			System.out.println("\nQuery Result for '" + filteredQ + "':");
			for (Map<String, Object> row : filteredResults) {
				System.out.println(row);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
