/**
 * 
 * @author Bruce Giese
 * @version 0.1
 * 
 * This reads the ever turking story database and create the next Mechanical Turk hit
 * for creating the next sentence in the story.
 * 
 * Note that this requires JDOM 2.0.5 or later (see http://www.jdom.org)
 *
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.jdom2.*;
import org.jdom2.input.*;
import org.jdom2.output.*;
import org.jdom2.util.*;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.jdom2.filter.*;

// TODO Change all the print statements to lo4j log statements.
public class CreateSentenceHit {
	static final String SENTENCE_TEMPLATE_FILE = "./sentenceTemplate.xml";
	static final String OUTPUT_FILE_NAME = "./sentence.xml";

	public static void main(String[] args) {
		SentenceHitXML hit = new SentenceHitXML(SENTENCE_TEMPLATE_FILE, OUTPUT_FILE_NAME);
		
		hit.addHitText("This is some story text");
		
		File hitFile = hit.getHitFile();

	}
}

/**
 * 
 * @author Bruce Giese
 * 
 * This object is a Mechanical Turk HIT for requesting someone to write
 * a sentence for the story.  Most of the HIT text is static and contained
 * in an XML file which is passed to the constructor.
 *
 */
class SentenceHitXML {
	Document doc;
	Element root;
	Namespace ns;
	String SentenceHitFile;
	String SentenceTemplateFile;
	File hitFile;
	
	public SentenceHitXML( String SentenceTemplateFile, String SentenceHitFile) {
		this.SentenceTemplateFile = SentenceTemplateFile;
		this.SentenceHitFile = SentenceHitFile;
		
		try {
			doc = new SAXBuilder().build(SentenceTemplateFile);
			root = doc.getRootElement();
			ns = root.getNamespace("my");
			
			hitFile = new File(SentenceHitFile);
			
		} catch(IOException ioe) {
			System.out.println("SentenceHitXML constructor, reading " + SentenceTemplateFile);
			System.out.println(ioe);
		} catch(JDOMException jde) {
			System.out.println("SentenceHitXML constructor, SAX parsing " + SentenceTemplateFile);
			System.out.println(jde);
		}
	}
	
	/**
	 * 	Add the dynamic part of the HIT, which is essentially the
	 *  most recent part of the story.  The static part of the hit
	 *  is contained in the sentence template file.
	 */
	public void addHitText(String StoryText) {
		try {
			Element question = root.getChild("Question",ns);
			if( question == null ) {
				throw new Exception("The XML file doesn't contain a proper Question element");
			}
			System.out.println("got question: " + question.getName());
			Element qc = question.getChild("QuestionContent", ns);
			if( qc == null ) {
				throw new Exception("The XML file doesn't contain a proper QuestionContent element");
			}
			System.out.println("got question content: " + qc.getName());
			Element txt = qc.getChild("Text", ns);
			if( txt == null ) {
				throw new Exception("The XML file's QuestionContent element doesn't have a Text element");
			}
			System.out.println("got text element: " + txt.getName());
			
			txt.addContent(StoryText);
			
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.output(doc, new FileOutputStream(hitFile));

		} catch(IOException ioe) {
			System.out.println("Opening " + SentenceTemplateFile + ": " + ioe);
		} catch(JDOMException jde) {
			System.out.println("Trying to build JDOM from " + SentenceTemplateFile + ": " + jde);
		} catch(Exception e) {
			System.out.println("Something happen: " + e);
		}
	}
	
	/**
	 * @return the file containing the hit as a File object.
	 */
	public File getHitFile() {
		return hitFile;
	}
}
