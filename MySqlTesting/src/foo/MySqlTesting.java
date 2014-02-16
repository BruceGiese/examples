package foo;

/**
 * Connects to a mysql server "ever-turking-story" database and fetches the story.
 * 
 * You need to add mysql-connector-java-5.1.29-bin.jar or later to your classpath.
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/*
 * Notice, do not import com.mysql.jdbc.*
 * or you will have problems!
 */

public class MySqlTesting {
	static final String TEXT_TO_ADD = "And they all lived happily ever after.";

	public static void main(String[] args) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		// TODO Change these print statements into log statements using a good logger.
		System.out.println("getting an instance of the JDBC driver manager...");
        try {
            // The newInstance() call is a work around for some
            // broken Java implementations
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            System.out.println("    " + ex);
            System.exit(-1);
        }
        
        System.out.println("connecting to the SQL datbase...");
        try {
        	conn = DriverManager.getConnection("jdbc:mysql://localhost/turking_story?" +
                    "user=turk&password=turk");
        	
        } catch (SQLException se) {
            System.out.println("    SQLException: " + se.getMessage());
            System.out.println("    SQLState: " + se.getSQLState());
            System.out.println("    VendorError: " + se.getErrorCode());
            System.exit(-2);
        }
        
        
        try {
        	int lastSentence = 0;	// Start with the initial value in case there are no sentences in the story yet.
        	int rowCount = 0;
        	String str;
        	
        	stmt = conn.createStatement();

        	System.out.println("execute query to get all the sentences in the story....");
        	rs = stmt.executeQuery("SELECT sentence from sentences WHERE accepted=1 ORDER BY sentence_number");
        	
            System.out.print("display the query results...\n    ");
        	if( rs.first() ) {
        		do {
        			// TODO send this out to a file rather than the console.
        			System.out.print(rs.getString("sentence") + " ");
        		} while( rs.next() );
        		
        		// I could have done this above, but in the actual project I need to do it like this:
        		System.out.println("\n\ndetermining the last sentence in the story...");
        		rs.close();
        		rs = stmt.executeQuery("SELECT MAX(sentence_number) from sentences");
        		if( rs.first() ) {
        			lastSentence = rs.getInt(1);	// There can be only one
        			System.out.println("    The last sentence number is " + lastSentence);
        		} else {
        			throw new Exception("MySqlTesting: There was no maximum sentence_id");
        		}
        	} else {
        		System.out.println("    There are no sentences in the story yet.");
        	}
        	
        	str = "INSERT INTO sentences (sentence,sentence_number,worker_id,votes,accepted) VALUES (\"" + TEXT_TO_ADD + "\"," + Integer.toString(lastSentence+1) + ",0,1,1)";
        	System.out.println("updating the database using: " + str);
        	rowCount = stmt.executeUpdate(str);
        	if( rowCount == 1) {
        		System.out.println("Success!");
        	} else {
        		System.out.println("We expected to update 1 row, but it updated " + rowCount);
        	}

        } catch (SQLException qe) {
            System.out.println("    SQLException: " + qe.getMessage());
            System.out.println("    SQLState: " + qe.getSQLState());
            System.out.println("    VendorError: " + qe.getErrorCode());
        } catch (Exception e) {
        	System.out.println(e);
        } finally {
        	if(rs != null) {
        		try {
        			rs.close();					// free up resources
        		} catch(SQLException sqle) {} 	// at least we tried
        		
        		rs = null;
        	}
        	
        	if(stmt != null) {
        		try {
        			stmt.close();				// free up resources
        		} catch(SQLException sqle) {}	// at least we tried
        		
        		stmt = null;
        	}
        }
	}
}
