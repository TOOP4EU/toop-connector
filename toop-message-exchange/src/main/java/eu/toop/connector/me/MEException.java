package eu.toop.connector.me;

/**
 * A separate runtime exception to make it easier for the users to distinguish between the 'source path' to the
 * underlying problem.
 *
 * @author yerlibilgin
 */
public class MEException extends IllegalStateException {

  public MEException(String message) {
    super(message);
  }


  public MEException(String message, Throwable cause) {
    super(message, cause);
  }


  public MEException(Throwable cause) {
    super(cause);
  }

}
