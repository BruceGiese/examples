import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 
 * @author Bruce Giese
 * 
 * This class is the dynamic part of the Mechanical Turk HIT for requesting someone to write
 * a sentence for the story.  Most of the HIT text is static and contained
 * in an XML file.  The dynamic part is single String object which can be obtained using getString().
 * 
 * Note that this requires adding mysql-connector-java 5.1 jar file into the build path.
 * 
 * Database requirements:
 * This requires a MySQL database to be set up containing a table called "sentences".
 * The table needs the following columns:
 *		sentence_id must be an auto-incrementing INT primary key
 *		sentence must be a TEXT field and it contains an actual potential story sentence,
 *			depending on whether it won the vote to get accepted into the story.
 * 		sentence number must be an INT.  It contains the sentence number within the story.
 * 			Note that several sentences may have the same sentence_number, but only one of
 *			them can be accepted into the story.
 *		worker_id must be a TEXST field.  It contains the worker identifier of who wrote
 *			this particular sentence.  This is needed for paying, rewarding, crediting.
 *		votes must be an INT field and it contains the number of votes the sentence received.
 *			Note that this sentence competes with all sentences with the same sentence_number.
 *		accepted is a BIT.  0 means not accepted into the story.  1 means accepted.  In the
 *			event of a tie vote, any sort of choice is acceptable and gets reflected in this bit.
 * The database needs to have a user account which has read access by this class.  You will
 * also need to give write access to the class which handles the results and the class which
 * handles the voting results.
 */
public class DynamicSentenceHitSection {
	final static int NUMBER_OF_SENTENCES_TO_DISPLAY = 7;	// These are the last N sentences in the story, so far.
	private Connection conn;
	
	/**
	 * 
	 * @param databaseURL	the location of the database which has already been set up with a sentences table.
	 * @param username		the database username to use for reading the sentences.
	 * @param password		the password associated with the username.
	 * @throws DynamicSentenceHitException		This exception was created to avoid having to throw multiple types.
	 */
	public DynamicSentenceHitSection(String databaseURL, String username, String password) throws DynamicSentenceHitException {
        try {
        	conn = DriverManager.getConnection("jdbc:" + databaseURL + "?user=" + username + "&password=" + password);
        	
        } catch (SQLException se) {
            throw new DynamicSentenceHitException("SQLException: " + se.getMessage() +
            		", SQLState: " + se.getSQLState() + ", VenorError: " + se.getErrorCode());
        }
	}
	
	/**
	 * 
	 * @return 		The most recent N sentences in the story (where N is defined within the class).
	 * @throws DynamicSentenceHitException		This exception was created to avoid having to throw multiple types.
	 */
	public String getString() throws DynamicSentenceHitException {
		Statement stmt = null;
		ResultSet rs = null;
		StringBuilder result = new StringBuilder();
		
        try {
        	int sentencesToDiscard = -1;		// We only display the last N sentences, so we need to discard length-N of them.
        	
        	stmt = conn.createStatement();
        	
    		// TODO log message "determining the last sentence in the story..."
    		
    		rs = stmt.executeQuery("SELECT MAX(sentence_number) from sentences");
    		if( rs.first() ) {
    			int lastSentence = rs.getInt(1);	// There can be only one last sentence
    			// TODO log message "The last sentence number is " + lastSentence
    			
    			if( lastSentence < (NUMBER_OF_SENTENCES_TO_DISPLAY - 1) ) {		// the index is zero based
    				throw new DynamicSentenceHitException("There are not enough sentences in the database yet.  You need at least" + NUMBER_OF_SENTENCES_TO_DISPLAY);
    			}
    			sentencesToDiscard = lastSentence - (NUMBER_OF_SENTENCES_TO_DISPLAY - 1);		// the index is zero based
    			
    		} else {
    			throw new DynamicSentenceHitException("There was no maximum sentence_id, the database is corrupted, but probably fixable.");
    		}

        	// TODO log message "execute query to get all the accepted sentences in the story..."
        	// This could become a problem with memory use if the story gets too long.
        	rs.close();
        	rs = stmt.executeQuery("SELECT sentence from sentences WHERE accepted=1 ORDER BY sentence_number");
        	
            // TODO log message "collect the query results...\n"
        	if( rs.first() ) {
        		do {
        			if(sentencesToDiscard-- > 0) continue;
        			result.append(rs.getString("sentence") + " ");  // for simplicity of code, we'll allow a final trailing space.
        		} while( rs.next() );
        		
        	} else {
        		throw new DynamicSentenceHitException("Internal logic error: it seemed like there were sentences, but there are none.");
        	}
        	
        	return result.toString();

        } catch (SQLException qe) {
            throw new DynamicSentenceHitException("SQLException: " + qe.getMessage() +
            		", SQLState: " + qe.getSQLState() + ", VenorError: " + qe.getErrorCode());
            
        } catch (Exception e) {
        	throw new DynamicSentenceHitException("Something went wrong: ", e);
        	
        } finally {
        	if(rs != null) {
        		try {
        			rs.close();					// Free up resources
        		} catch(SQLException sqle) {} 	// At least we tried
        		
        		rs = null;
        	}
        	
        	if(stmt != null) {
        		try {
        			stmt.close();				// Free up resources
        		} catch(SQLException sqle) {}	// At least we tried
        		
        		stmt = null;
        	}
        }
	}
}
