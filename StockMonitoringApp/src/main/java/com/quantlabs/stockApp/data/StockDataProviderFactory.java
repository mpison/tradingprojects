package com.quantlabs.stockApp.data;

import java.util.Map;

import okhttp3.OkHttpClient;

public class StockDataProviderFactory {
	private final OkHttpClient client;
	private final String alpacaApiKey;
	private final String alpacaApiSecret;
	private final String polygonApiKey;

	public StockDataProviderFactory(OkHttpClient client, String alpacaApiKey, String alpacaApiSecret,
			String polygonApiKey) {
		this.client = client;
		this.alpacaApiKey = alpacaApiKey;
		this.alpacaApiSecret = alpacaApiSecret;
		this.polygonApiKey = polygonApiKey;
	}

	public StockDataProvider getProvider(String providerName, ConsoleLogger logger) {
		switch (providerName) {
		case "Alpaca":
			return new AlpacaDataProvider(client, alpacaApiKey, alpacaApiSecret, logger);
		case "Yahoo":
			return new YahooDataProvider(client, logger);
		case "Polygon":
			return new PolygonDataProvider(client, polygonApiKey, logger);
		default:
			throw new IllegalArgumentException("Unknown provider: " + providerName);
		}
	}

	public StockDataProvider getProvider(String providerName, ConsoleLogger logger, Map<String, Object> config) {
		StockDataProvider provider = getProvider(providerName, logger);

		// Apply configuration if it's Alpaca provider
		if ("Alpaca".equals(providerName) && provider instanceof AlpacaDataProvider) {
			AlpacaDataProvider alpacaProvider = (AlpacaDataProvider) provider;
			if (config != null) {
				if (config.containsKey("adjustment")) {
					alpacaProvider.setAdjustment((String) config.get("adjustment"));
				}
				if (config.containsKey("feed")) {
					alpacaProvider.setFeed((String) config.get("feed"));
				}
			}
		}

		return provider;
	}
}