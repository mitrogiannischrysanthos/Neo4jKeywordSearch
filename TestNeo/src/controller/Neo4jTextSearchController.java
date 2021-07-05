package controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.neo4j.driver.Record;
import model.DataSet;
import view.GUIOfNeo4jTextSearch;

public class Neo4jTextSearchController {
	private HashMap<String, DataSet> dataSets= new HashMap<String,DataSet>();
	private DataSetFactory dataSetFactory;
	private GUIOfNeo4jTextSearch GUIOfNeo4jTextSearch;
	
	public Neo4jTextSearchController(GUIOfNeo4jTextSearch guiOfNeo4jTextSearch) {
		this.dataSetFactory = new DataSetFactory();
		this.GUIOfNeo4jTextSearch = guiOfNeo4jTextSearch;
		InitializeDataSet();
	}
	
	public void InitializeDataSet() {
		dataSets.put("IMDB",dataSetFactory.createPlot("IMDB"));
	}
	
	public void enact() throws IOException {
		dataSets.get(GUIOfNeo4jTextSearch.getTypeOfDataSet()).executeQuery(GUIOfNeo4jTextSearch.getTextOfUserTextField(), GUIOfNeo4jTextSearch.getLimitTextArea());
	}
	
	public List<Record> getQueryResults() {
		return dataSets.get(GUIOfNeo4jTextSearch.getTypeOfDataSet()).getQueryResults();
	}
	
	public float getQueryExecutionTime() {
		return dataSets.get(GUIOfNeo4jTextSearch.getTypeOfDataSet()).getQueryExecutionTime();
	}
	
	public List<Record> getQueryYMALResults() {
		return dataSets.get(GUIOfNeo4jTextSearch.getTypeOfDataSet()).getQueryYMALResults();
	}
	
	public float getQueryExecutionTimeYMAL() {
		return dataSets.get(GUIOfNeo4jTextSearch.getTypeOfDataSet()).getQueryExecutionTimeYMAL();
	}
}