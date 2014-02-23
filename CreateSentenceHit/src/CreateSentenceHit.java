/**
 * 
 * @author Bruce Giese
 * @version 0.1
 * 
 * This reads the ever turking story database and create the next Mechanical Turk hit
 * for requestion workers to create the next sentence in the story.
 * 
 * Note that this requires JDOM 2.0.5 or later (see http://www.jdom.org)
 * 
 */

import java.io.File;

// TODO Change all the print statements to lo4j 2.0 log statements.
public class CreateSentenceHit {
	static final String SENTENCE_TEMPLATE_FILE = "./sentenceTemplate.xml";
	static final String OUTPUT_FILE_NAME = "./sentence.xml";
	
	static final String DATABASE = "mysql://localhost/turking_story";
	static final String USERNAME = "turk";
	static final String PASSWORD = "turk";

	public static void main(String[] args) {
		SentenceHitXML hit = new SentenceHitXML(SENTENCE_TEMPLATE_FILE, OUTPUT_FILE_NAME);

		try {
			DynamicSentenceHitSection dhs = new DynamicSentenceHitSection(DATABASE, USERNAME, PASSWORD);
			hit.addHitText(dhs.getString());
		
		} catch( DynamicSentenceHitException dshe) {
			System.out.println("Something went wrong when creating the dynamic part of the sentence creation hit.");
			System.out.println("..." + dshe);
		}
	}
}
