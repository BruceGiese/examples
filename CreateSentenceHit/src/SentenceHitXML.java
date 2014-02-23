import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;


/**
 * 
 * @author Bruce Giese
 * 
 * This class is a Mechanical Turk HIT for requesting someone to write
 * a sentence for the story.  Most of the HIT text is static and contained
 * in an XML file which is passed to the constructor.
 *
 */
public class SentenceHitXML {
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
			// TODO log message "got question: " + question.getName()
			Element qc = question.getChild("QuestionContent", ns);
			if( qc == null ) {
				throw new Exception("The XML file doesn't contain a proper QuestionContent element");
			}
			// TODO log message "got question content: " + qc.getName()
			Element txt = qc.getChild("Text", ns);
			if( txt == null ) {
				throw new Exception("The XML file's QuestionContent element doesn't have a Text element");
			}
			// TODO log message "got text element: " + txt.getName()
			
			txt.addContent(StoryText);
			
			// TODO log message "write out the HIT to the file" + hitFile
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
