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
        
        System.out.println("execute query to get all the sentences in the story");
        try {
        	stmt = conn.createStatement();
        	rs = stmt.executeQuery("SELECT sentence from sentences WHERE accepted=1 ORDER BY sentence_number");
        	
            System.out.println("display the query results...");
        	if( rs.first() ) {
        		do {
        			// TODO send this out to a file rather than the console.
        			System.out.print(rs.getString("sentence") + " ");
        		} while( rs.next() );
        		System.out.println("\nTo be continued");
        		
        	} else {
        		System.out.println("    There are no sentences in the story yet.");
        	}

        } catch (SQLException qe) {
            System.out.println("    SQLException: " + qe.getMessage());
            System.out.println("    SQLState: " + qe.getSQLState());
            System.out.println("    VendorError: " + qe.getErrorCode());
        	System.exit(-3);
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
        
        System.out.println("Success!");
	}
}
