package aparmar.naisiteengine.utils.yaml;

public class YamlParseException extends RuntimeException {
	private static final long serialVersionUID = 7769254038764405383L;

	/**
	   * Creates exception with the specified message. If you are wrapping another exception, consider
	   * using {@link #YamlParseException(String, Throwable)} instead.
	   *
	   * @param msg error message describing a possible cause of this exception.
	   */
	  public YamlParseException(String msg) {
	    super(msg);
	  }

	  /**
	   * Creates exception with the specified message and cause.
	   *
	   * @param msg error message describing what happened.
	   * @param cause root exception that caused this exception to be thrown.
	   */
	  public YamlParseException(String msg, Throwable cause) {
	    super(msg, cause);
	  }

	  /**
	   * Creates exception with the specified cause. Consider using
	   * {@link #YamlParseException(String, Throwable)} instead if you can describe what happened.
	   *
	   * @param cause root exception that caused this exception to be thrown.
	   */
	  public YamlParseException(Throwable cause) {
	    super(cause);
	  }
}
