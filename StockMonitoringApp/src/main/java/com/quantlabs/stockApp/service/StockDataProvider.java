package com.quantlabs.stockApp.service;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.exception.StockApiException;
import java.util.List;

public abstract class StockDataProvider {
	public List<PriceData> getDowJones() throws StockApiException{return null;}

	public List<PriceData> getNasdaq() throws StockApiException{return null;}

	public List<PriceData> getSP500() throws StockApiException{return null;}

	public List<PriceData> getRussell2000() throws StockApiException{return null;}

	public List<PriceData> getPennyStocksByVolume() throws StockApiException{return null;}

	public  List<PriceData> getPennyStocksByPercentChange() throws StockApiException{return null;}
}