package view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.neo4j.driver.Record;
import controller.Neo4jTextSearchController;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Pagination;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class GUIOfNeo4jTextSearch {
	public ChoiceBox databaseChoiceBox;
	public TextField userTextField;
	public Button searchButton;
	public TextArea numberOfResults;
	public TextArea executionTime;
	public TextArea limitTextArea;
	public Pagination defaultPagination;
	public TextArea numberOfYMALResults;
	public TextArea executionTimeYMAL;
	public Pagination paginationYMAL;
	public CheckBox yCheckBox;
	private String typeOfDataSet;
	private Neo4jTextSearchController neo4jTextSearchController;
	private ArrayList<String[]> contentsForHyperLinks;
	private List<Record> queryResult;
	
	public void initialize() {
		initializeFields();
		initializePaginationDefault();
		initializeDatabaseChoiceBox();
	}
	
	public void initializeFields() {
		neo4jTextSearchController = new Neo4jTextSearchController(this);
		contentsForHyperLinks = new ArrayList<String[]>();
	}
	
	public void initializePaginationDefault() {
		defaultPagination.setPageCount(1);
		paginationYMAL.setPageCount(1);
	}
	
	public void initializeDatabaseChoiceBox() {
		databaseChoiceBox.getItems().addAll("IMDB");
		databaseChoiceBox.setValue("IMDB");
	}
	
	public void handleSearchButtonClick() throws IOException {
		this.typeOfDataSet = databaseChoiceBox.getValue().toString();
		contentsForHyperLinks.clear();
		paginationYMAL.setPageCount(1);
		neo4jTextSearchController.enact();
		queryResult = neo4jTextSearchController.getQueryResults();
		executionTime.setText(neo4jTextSearchController.getQueryExecutionTime() + "");
		numberOfResults.setText(queryResult.size() + "");
		defaultPagination.setPageCount(Math.round(queryResult.size()/(float)itemsPerPage()));
		processQueryResults();
		setPaginationElements(queryResult.size(), defaultPagination);
		if (yCheckBox.isSelected()) {
			contentsForHyperLinks.clear();
			queryResult = neo4jTextSearchController.getQueryYMALResults();
			executionTimeYMAL.setText(neo4jTextSearchController.getQueryExecutionTimeYMAL() + "");
			numberOfYMALResults.setText(queryResult.size() + "");
			paginationYMAL.setPageCount(Math.round(queryResult.size()/(float)itemsPerPage()));
			processQueryResults();
			setPaginationElements(queryResult.size(), paginationYMAL);
		}
	}
	
	public int itemsPerPage() {
        return 8;
    }

	public void processQueryResults() {
		for (Record tuple : queryResult) {
    		Map<String,Object> row = tuple.asMap();
    		String hyperLinkLabel = "";
    		String hyperLinkText = "";
    		int person = 0;
    		
			for (Entry<String,Object> column : row.entrySet())
    		{
				if (column.getValue() != null && column.getValue().toString().equals("[Person]")) {
					person = 1;
					continue;
				} else if (column.getValue() != null && column.getValue().toString().equals("[Movie]")) {
					person = 0;
					continue;
				} else if (column.getValue() != null && column.getValue().toString().equals("[Tuple]")) {
					person = 2;
					continue;
				}
				
				if (column.getValue() != null) {
					hyperLinkText += column.getValue() + "\n";
					if (person == 1 && column.getKey().toString().equals("pnode")) {
				        hyperLinkText = configureHyperLinkText(hyperLinkText);
				        setContentsForHyperLinks(getNameForHyperLink("name", column.getValue().toString()), hyperLinkText);
					} else if (person == 0 && column.getKey().toString().equals("pnode")) {
						hyperLinkText = configureHyperLinkText(hyperLinkText);
						setContentsForHyperLinks(getNameForHyperLink("title", column.getValue().toString()), hyperLinkText);
					} else if (person == 2) {
						if (column.getKey().toString().equals("lnode")) {
							hyperLinkLabel = "";
						} else if (!column.getKey().toString().equals("score")) {
							hyperLinkLabel += column.getValue() + " ";
						} else if (column.getKey().toString().equals("score")) {
							hyperLinkText = configureHyperLinkText(hyperLinkText.substring(0, hyperLinkText.lastIndexOf('\n', hyperLinkText.length() - 2)));
							setContentsForHyperLinks(hyperLinkLabel, hyperLinkText);
						}
					}
				}
    		}
    	}
	}
	
	public String configureHyperLinkText(String hyperLinkText) {
		return hyperLinkText.replaceAll("=", ": ");
	}
	
	public void setContentsForHyperLinks(String hyperLinkLabel, String hyperLinkText) {
		String[] hyperLinkContents = {hyperLinkLabel, hyperLinkText};
		contentsForHyperLinks.add(hyperLinkContents);
	}
	
	private String getNameForHyperLink(String fieldName, String text) {
		Pattern p = Pattern.compile(fieldName + "=(.*?),");
		Matcher m = p.matcher(text);
		m.find();
		return m.group(1);
	}
	
	public void setPaginationElements(int queryResultSize, Pagination pagination) {
		pagination.setPageCount(Math.round((int) Math.ceil(queryResultSize/(float) itemsPerPage())));
		pagination.setPageFactory(new Callback<Integer, Node>() {
            @Override
            public Node call(Integer pageIndex) {
                return createPage(pageIndex, contentsForHyperLinks);
            }
        });
	}
	
	public VBox createPage(int pageIndex, ArrayList<String[]> contentsForHyperLinks) {
        VBox box = new VBox(5);
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setEditable(false);
        int page = pageIndex * itemsPerPage();
        for (int i = page; i < page + itemsPerPage(); i++) {
        	final int position = i;
        	if (position > contentsForHyperLinks.size() - 1 || position > Integer.parseInt(getLimitTextArea()) - 1) {
        		break;
        	}
            VBox element = new VBox();
            Hyperlink link = new Hyperlink(contentsForHyperLinks.get(position)[0]);
            link.setOnAction(e -> showHyperLinks(textArea, contentsForHyperLinks.get(position)[1]));
            element.getChildren().addAll(link);
            box.getChildren().add(element);
        }
        box.getChildren().add(textArea);
        return box;
    }
	
	public void showHyperLinks(TextArea textArea, String hyperLinkText) {
		textArea.setText(hyperLinkText);
	}
	
	public String getTypeOfDataSet() {
		return typeOfDataSet;
	}
	
	public String getTextOfUserTextField() {
		return userTextField.getText();
	}
	
	public String getLimitTextArea() {
		return limitTextArea.getText();
	}
}