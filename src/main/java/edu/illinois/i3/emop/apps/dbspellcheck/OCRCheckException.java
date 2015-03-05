package edu.illinois.i3.emop.apps.dbspellcheck;

@SuppressWarnings("serial")
public class OCRCheckException extends Exception {

	public OCRCheckException(String msg) {
		super(msg);
	}

	public OCRCheckException(Throwable cause) {
		super(cause);
	}

	public OCRCheckException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
