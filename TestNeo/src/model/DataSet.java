package model;

import java.io.IOException;
import java.util.List;
import org.neo4j.driver.Record;

public interface DataSet {
	public void executeQuery(String searchText, String limitOfResults) throws IOException;
	public List<Record> getQueryResults();
	public float getQueryExecutionTime();
	public List<Record> getQueryYMALResults();
	public float getQueryExecutionTimeYMAL();
}
