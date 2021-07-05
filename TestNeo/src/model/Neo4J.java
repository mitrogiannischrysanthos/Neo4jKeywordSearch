package model;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionWork;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Neo4J implements AutoCloseable
{
    private Driver driver = null;
    private List<Record> queryResults;
    private float queryExecutionTime;

    public Neo4J( String uri, String user, String password)
    {
        driver = GraphDatabase.driver( uri, AuthTokens.basic( user, password ) );
    }

    @Override
    public void close() throws Exception
    {
        driver.close();
    }
    
    public String returnQueryAsString(String message, String userText)
    {
        try ( Session session = driver.session() )
        {
            String queryResult = session.writeTransaction( new TransactionWork<String>()
            {
                @Override
                public String execute( Transaction tx )
                {
                	Result result = tx.run(message);  
                	queryResults = result.list();
                	queryExecutionTime = (result.consume().resultAvailableAfter(TimeUnit.MILLISECONDS) + result.consume().resultConsumedAfter(TimeUnit.MILLISECONDS))/((float)1000);
                	return "";
                }
            } );
            return queryResult;
        }
    }
    
    public List<Record> getQueryResults() {
    	return queryResults;
    }
    
    public float getQueryExecutionTime() {
    	return queryExecutionTime;
    }
	
	public String markTheGivenWordsWithBold(String textToMark, String queryResult)
	{
		String arrayWithNeo4jQueryWords[] = queryResult.split(" ");
		String wordToMark;
		String wordsOfSearchText[] = textToMark.split(" ");
		for (int i=0; i<arrayWithNeo4jQueryWords.length; i++)
		{
			wordToMark = arrayWithNeo4jQueryWords[i].replaceAll("\\p{Punct}"," ");
			wordToMark = wordToMark.replaceAll("( )+", "");
			for (int j=0; j<wordsOfSearchText.length; j++)
			{
				if ((wordToMark.toLowerCase()).equals(wordsOfSearchText[j].toLowerCase()) && !wordsOfSearchText[j].equals(""))
				{
					arrayWithNeo4jQueryWords[i] = addChar(arrayWithNeo4jQueryWords[i], "<b>", arrayWithNeo4jQueryWords[i].toLowerCase().indexOf(wordsOfSearchText[j].toLowerCase()));
					arrayWithNeo4jQueryWords[i] = addChar(arrayWithNeo4jQueryWords[i], "</b>", arrayWithNeo4jQueryWords[i].toLowerCase().indexOf(wordsOfSearchText[j].toLowerCase()) + wordsOfSearchText[j].length());
					break;
				}
			}
		}
		
		return String.join(" ", arrayWithNeo4jQueryWords);
	}
	
	public String addChar(String word, String chars, int position) {
	    StringBuilder stringBuilder = new StringBuilder(word);
	    stringBuilder.insert(position, chars);
	    return stringBuilder.toString();
	}
}