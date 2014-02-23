
/**
 * 
 * @author Bruce Giese
 *
 * Define an exception for anything related to Dynamic Sentence HIT text generation.
 * 
 */
public class DynamicSentenceHitException extends Exception {
	static final long serialVersionUID = 1;
	static final String EXCEPTION_NAME = "DynamicSentenceHitException";
	
	public DynamicSentenceHitException(String message) {
		super(EXCEPTION_NAME + ": " + message);
	}
	
	public DynamicSentenceHitException(String message, Throwable throwable) {
		super(EXCEPTION_NAME + ": " + message, throwable);
	}
	
	@Override
	public String getMessage() {
		return EXCEPTION_NAME + ": " + super.getMessage();
	}
}
