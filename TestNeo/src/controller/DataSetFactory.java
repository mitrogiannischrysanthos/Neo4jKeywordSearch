package controller;

import model.DataSet;
import model.IMDBDataSet;

public class DataSetFactory {
	public DataSetFactory() {}
	
	public DataSet createPlot(String plotKey) {
		switch(plotKey) {
			case "IMDB": 
				return new IMDBDataSet();
			default:
				return null;
		}
	}
}
