
public class InvalidBlockException extends Exception{
	public InvalidBlockException() {
        super();
    }
	// Constructor that accepts a message
    public InvalidBlockException(String message) {
        super(message);
    }

    // Constructor that accepts a message and a cause
    public InvalidBlockException(String message, Throwable cause) {
        super(message, cause);
    }
}
