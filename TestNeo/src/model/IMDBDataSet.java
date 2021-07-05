package model;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.driver.Record;

public class IMDBDataSet implements DataSet{
	private Neo4J neo4j;
	private List<Record> queryResults;
	private List<Record> queryYMALResults;
	private float queryExecutionTime;
	private float queryExecutionTimeYMAL;
	
	public IMDBDataSet() {
		neo4j = new Neo4J( "bolt://localhost:7687", "neo4j", "****");
	}
	
	public void executeQuery(String searchText, String limitOfResults) throws IOException {
		if (containsPairOfQuotes(searchText)) {
			ArrayList<String> userKeyWords = new ArrayList<String>();
			userKeyWords = getStringsBetweenSingleQuotes(searchText, userKeyWords);
			
			String queryText = generateQueryExtensions(userKeyWords, limitOfResults);
			neo4j.returnQueryAsString(queryText, searchText);
			queryResults = neo4j.getQueryResults();
			queryExecutionTime = neo4j.getQueryExecutionTime();
			
			String queryTextYMAL = generateQueryYMAL(userKeyWords, limitOfResults);
			neo4j.returnQueryAsString(queryTextYMAL, searchText);
			queryYMALResults = neo4j.getQueryResults();
			queryExecutionTimeYMAL = neo4j.getQueryExecutionTime();
			
			createGraphVisualizationExtension(queryText);
			
		} else {
			String initalTextWithoutPunctuations = removePunctuationFromInitialText(searchText);
			String queryText = generateQueryBasedOnTypeDefault(configureQueryWithBoosting(initalTextWithoutPunctuations), initalTextWithoutPunctuations, limitOfResults);
			String neo4jQueryResult = neo4j.returnQueryAsString(queryText, searchText);
			queryResults = neo4j.getQueryResults();
			queryExecutionTime = neo4j.getQueryExecutionTime();
			neo4j.markTheGivenWordsWithBold(initalTextWithoutPunctuations, neo4jQueryResult);
			createGraphVisualizationDefault(queryText);
		}
	}
	
	public boolean containsPairOfQuotes(String searchText) {
		return searchText.charAt(0) == '\'' && searchText.charAt(searchText.length()-1) == '\'';
	}
	
	public String removePunctuationFromInitialText(String searchText) {
		return searchText.replaceAll("\\p{Punct}"," ").replaceAll("( )+", " ");
	}
	
	public ArrayList<String> getStringsBetweenSingleQuotes(String searchText, ArrayList<String> userKeyWords) {
		Pattern p = Pattern.compile("\'(.*?)\'");
		Matcher m = p.matcher(searchText);
		while (m.find()) {
			userKeyWords.add(m.group(1));
		}
		return userKeyWords;
	}
	
	//Three statements for each one of the three extensions.
	public String generateQueryExtensions(ArrayList<String> userKeyWords, String limitOfResults) {
		return  "WITH [" + createListForQueryExtensions(userKeyWords, "'") + "] as names\r\n" + 
				"MATCH (person:Person)-[]-(movie:Movie)\r\n" + 
				"WHERE person.name in names\r\n" + 
				"WITH movie, size(names) as sizeOfListWithNames, count(DISTINCT person) as count\r\n" + 
				"WHERE count = sizeOfListWithNames\r\n" + 
				"RETURN labels(movie) as lnode, apoc.map.removeKeys(movie {.*}, [\"known_for_titles\", \"imdb_name_id\", \"imdb_title_id\", \"pagerank\"]) as pnode, null as relationship, null as lmovie, null as pmovie, null as score\r\n" +
				"LIMIT " + limitOfResults + " \r\n" +
				"UNION\r\n" + 
				"WITH [" + createListForQueryExtensions(userKeyWords, "'") + "] as titles\r\n" + 
				"MATCH (person:Person)-[]-(movie:Movie)\r\n" + 
				"WHERE movie.title in titles\r\n" + 
				"WITH person, size(titles) as sizeOfListWithTitles, count(DISTINCT movie) as count\r\n" + 
				"WHERE count = sizeOfListWithTitles\r\n" + 
				"RETURN labels(person) as lnode, apoc.map.removeKeys(person {.*}, [\"known_for_titles\", \"imdb_name_id\", \"imdb_title_id\", \"pagerank\"]) as pnode, null as relationship, null as lmovie, null as pmovie, null as score\r\n" +
				"LIMIT " + limitOfResults + " \r\n" +
				"UNION\r\n" + 
				"CALL db.index.fulltext.queryRelationships(\"charactersIndex\", '" + createListForQueryExtensions(userKeyWords, "\"") + "') YIELD relationship, score\r\n" + 
				"UNWIND relationship as rel\r\n" + 
				"MATCH (person:Person)-[rel]-(movie:Movie)\r\n" + 
				"RETURN '[Tuple]' as lnode, person.name as pnode, ' - ' + rel.characters as relationship, null as lmovie, ' - ' + movie.title as pmovie, score\r\n" +
				"LIMIT " + limitOfResults;
	}
	
	//Three statements for each one of the three YMAL approaches.
	public String generateQueryYMAL(ArrayList<String> userKeyWords, String limitOfResults) {
		return  "MATCH (person:Person)-[]-(movie:Movie)\r\n" + 
				"WHERE person.name IN [" + createListForQueryExtensions(userKeyWords, "'") + "]\r\n" + 
				"WITH split(movie.genre, \",\") as listOfGenre\r\n" + 
				"UNWIND listOfGenre as genre\r\n" + 
				"WITH trim(genre) as gen\r\n" + 
				"WITH gen, count(gen) as count ORDER BY count DESC LIMIT 1\r\n" + 
				"MATCH (movie:Movie)\r\n" + 
				"WITH movie, split(movie.actors, \",\")+split(movie.director, \",\") as cast\r\n" + 
				"WHERE movie.genre CONTAINS gen AND NOT any(x IN cast WHERE trim(x) IN [" + createListForQueryExtensions(userKeyWords, "'") + "])\r\n" + 
				"RETURN DISTINCT labels(movie) as lnode, apoc.map.removeKeys(movie {.*}, [\"known_for_titles\", \"imdb_name_id\", \"imdb_title_id\", \"pagerank\"]) as pnode, movie.avg_vote as score\r\n" +
				"ORDER BY movie.avg_vote DESC\r\n" + 
				"LIMIT 5\r\n" + 
				"UNION\r\n" + 
				"MATCH (node:Person)\r\n" + 
				"WHERE node.name IN [" + createListForQueryExtensions(userKeyWords, "'") + "]\r\n" + 
				"CALL apoc.path.expandConfig(node, {minLevel: 2, maxLevel: 2}) YIELD path\r\n" + 
				"WITH nodes(path)[2] as persons\r\n" + 
				"WITH persons, COUNT(persons) as count ORDER BY count DESC LIMIT 5\r\n" + 
				"MATCH (person:Person)-[]-(m:Movie)\r\n" + 
				"WHERE person.name IN [" + createListForQueryExtensions(userKeyWords, "'") + "]\r\n" + 
				"WITH persons, collect(m.title) as movieTitlesOfInputList\r\n" + 
				"MATCH (persons:Person)-[]-(movie:Movie)\r\n" + 
				"WHERE NOT movie.title IN movieTitlesOfInputList\r\n" + 
				"RETURN DISTINCT labels(movie) as lnode, apoc.map.removeKeys(movie {.*}, [\"known_for_titles\", \"imdb_name_id\", \"imdb_title_id\", \"pagerank\"]) as pnode, movie.avg_vote as score\r\n" +
				"ORDER BY movie.avg_vote DESC\r\n" + 
				"LIMIT 5\r\n" + 
				"UNION\r\n" + 
				"MATCH (node:Person)\r\n" + 
				"WHERE node.name IN [" + createListForQueryExtensions(userKeyWords, "'") + "]\r\n" + 
				"WITH split(node.known_for_titles, ',') as titlesIDs, node\r\n" + 
				"UNWIND titlesIDs as IDs\r\n" + 
				"MATCH (person:Person)-[]-(m:Movie)\r\n" + 
				"WHERE m.imdb_title_id = IDs AND person.known_for_titles CONTAINS IDs AND NOT person.name IN [" + createListForQueryExtensions(userKeyWords, "'") + "]\r\n" + 
				"RETURN DISTINCT labels(person) as lnode, apoc.map.removeKeys(person {.*}, [\"known_for_titles\", \"imdb_name_id\", \"imdb_title_id\", \"pagerank\"]) as pnode, person.pagerank as score\r\n" +
				"ORDER BY person.pagerank DESC\r\n" + 
				"LIMIT 5";
	}
	
	public String createListForQueryExtensions(ArrayList<String> userKeyWords, String quote) {
		String listWithUserKeyWords = "";
		for (String userKeyWord: userKeyWords) {
			listWithUserKeyWords += quote + userKeyWord + quote;
			if (userKeyWord != userKeyWords.get(userKeyWords.size()-1)) {
				listWithUserKeyWords += ", ";
			}
		}
		return listWithUserKeyWords;
	}
	
	public String configureQueryWithBoosting(String textToBoost) {
		String dbProperties[] = {"name", "title", "birth_name", "original_title", "place_of_birth", "country", "language", "description",  "genre", "year", "primary_profession"};
		String words[] = textToBoost.split(" ");
		String boostedText = "";
		for (int i=0; i<words.length; i++) {
			for (int j=0; j<dbProperties.length; j++) {
				if(dbProperties[j].equals("name") || dbProperties[j].equals("title") || dbProperties[j].equals("birth_name") || dbProperties[j].equals("original_title")) {
					boostedText += dbProperties[j] + ":" + words[i] + "^1.5";
				}else {
					boostedText += dbProperties[j] + ":" + words[i];
				}
				if( (i != words.length-1) || (j != dbProperties.length-1)) {
					boostedText += " OR ";
				}
			}
		}
		System.out.println(boostedText);
		return boostedText;
	}
	
	public String generateQueryBasedOnTypeDefault(String userTextWithBoosting, String userTextWithoutPunctuations, String limitOfResults) {
		String dbPropertiesInMovie[] = {"title", "original_title", "country", "language", "description","genre","year"};
		String dbPropertiesInPerson[] = {"name", "birth_name", "place_of_birth", "primary_profession"};
		String queryText = "CALL{\r\n" + 
			"	CALL db.index.fulltext.queryNodes(\"movieDataBase\", \"" + userTextWithBoosting + "\") YIELD node, score\r\n" + 
			"	RETURN labels(node) as lnode, apoc.map.removeKeys(node {.*}, [\"known_for_titles\", \"imdb_name_id\", \"imdb_title_id\", \"pagerank\"]) as pnode, null as relationship, null as lmovie, null as pmovie, score\r\n" + 
			"	UNION\r\n" + 
			"	CALL{\r\n" + 
			"		CALL db.index.fulltext.queryNodes(\"movieDataBase\", \"" + userTextWithBoosting + "\") YIELD node, score\r\n" + 
			WITH node as person, score\r\n" + 
			"		MATCH (person:Person)-[r]-(m:Movie)\r\n" + 
			configureQueryWithWhereNames(userTextWithoutPunctuations, dbPropertiesInMovie) +  
			"		RETURN person as node, r as relationship, m as movie, score\r\n" + 
			"		UNION\r\n" + 
			"   	CALL db.index.fulltext.queryNodes(\"movieDataBase\", \"" + userTextWithBoosting + "\") YIELD node, score\r\n" + 
			"		WITH node as movie, score\r\n" + 
			"		MATCH (m:Person)-[r]-(movie:Movie)\r\n" + 
			configureQueryWithWhereNames(userTextWithoutPunctuations, dbPropertiesInPerson) +  
			"		RETURN m as node, r as relationship, movie, score\r\n" + 
			"	}\r\n" + 
			"	RETURN '[Tuple]' as lnode, node.name as pnode, relationship, null as lmovie, movie.title as pmovie, max(score) as score\r\n" + 
			"}\r\n" + 
			"RETURN lnode, pnode, type(relationship), 'as character ' + relationship.characters + ' in movie ', lmovie, pmovie, score\r\n" + 
			"ORDER BY score DESC\r\n" +
			"LIMIT " + limitOfResults;
		
		return queryText;
	}
	
	public String configureQueryWithWhereNames(String textToConfigure, String dbProperties[]) {
		String properties[] = dbProperties;
		String words[] = textToConfigure.split(" ");
		String configuredText = "WHERE ";
		for (int i=0; i<words.length; i++) {
			for (int j=0; j<properties.length; j++) {
				configuredText += "m." + properties[j] + " =~ '(?i).*\\\\b" + words[i] + "\\\\b.*'\r\n";
				if( (i != words.length-1) || (j != properties.length-1)) {
					configuredText += " OR ";
				}
			}
		}
		System.out.println(configuredText);
		return configuredText;
	}
	
	public void createGraphVisualizationExtension(String queryText) {
		createTextFile(configureQueryToReturnNodesAndRelationshipsExtension(queryText));
		showVisualizationInBrowser();
	}
	
	private String configureQueryToReturnNodesAndRelationshipsExtension(String queryText) {
		String queryTextNew = queryText;
		queryTextNew = queryTextNew.replace("RETURN labels(movie) as lnode, apoc.map.removeKeys(movie {.*}, [\"known_for_titles\", \"imdb_name_id\", \"imdb_title_id\", \"pagerank\"]) as pnode, null as relationship, null as lmovie, null as pmovie, null as score\r\n", "RETURN null as person, null as relationship, movie\r\n");
		queryTextNew = queryTextNew.replace("RETURN labels(person) as lnode, apoc.map.removeKeys(person {.*}, [\"known_for_titles\", \"imdb_name_id\", \"imdb_title_id\", \"pagerank\"]) as pnode, null as relationship, null as lmovie, null as pmovie, null as score\r\n", "RETURN person, null as relationship, null as movie\r\n");
		queryTextNew = queryTextNew.replace("RETURN '[Tuple]' as lnode, person.name as pnode, ' - ' + rel.characters as relationship, null as lmovie, ' - ' + movie.title as pmovie, score\r\n", "RETURN person, relationship, movie\r\n");
		return queryTextNew;
	}
	
	public void createGraphVisualizationDefault(String queryText) {
		createTextFile(configureQueryToReturnNodesAndRelationshipsDefault(queryText));
		showVisualizationInBrowser();
	}
	
	private String configureQueryToReturnNodesAndRelationshipsDefault(String queryText) {
		String queryTextNew = queryText;
		queryTextNew = queryTextNew.replace("RETURN labels(node) as lnode, apoc.map.removeKeys(node {.*}, [\"known_for_titles\", \"imdb_name_id\", \"imdb_title_id\", \"pagerank\"]) as pnode, null as relationship, null as lmovie, null as pmovie, score\r\n", "RETURN node, null as relationship, null as movie, score\r\n");
		queryTextNew = queryTextNew.replace("RETURN '[Tuple]' as lnode, node.name as pnode, relationship, null as lmovie, movie.title as pmovie, max(score) as score\r\n", "RETURN node, relationship, movie, max(score) as score\r\n");
		queryTextNew = queryTextNew.replace("RETURN lnode, pnode, type(relationship), 'as character ' + relationship.characters + ' in movie ', lmovie, pmovie, score\r\n" + 
				   "ORDER BY score DESC\r\n", "RETURN node, relationship, movie\r\n");
		return queryTextNew;
	}
	
	private void showVisualizationInBrowser() {
		try {
			File htmlFile = new File("NewFile.html");
			Desktop.getDesktop().browse(htmlFile.toURI());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void createTextFile(String text) {
		try {
			PrintWriter writer = new PrintWriter("searchWords.txt", "UTF-8");
			writer.println(text);
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public List<Record> getQueryResults() {
		return queryResults;
	}
	
	public float getQueryExecutionTime() {
    	return queryExecutionTime;
    }
	
	public List<Record> getQueryYMALResults() {
		return queryYMALResults;
	}
	
	public float getQueryExecutionTimeYMAL() {
    	return queryExecutionTimeYMAL;
    }
}