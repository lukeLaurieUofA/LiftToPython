
public class InvalidLineException extends Exception{
	public InvalidLineException() {
        super();
    }
	// Constructor that accepts a message
    public InvalidLineException(String message) {
        super(message);
    }

    // Constructor that accepts a message and a cause
    public InvalidLineException(String message, Throwable cause) {
        super(message, cause);
    }
}
