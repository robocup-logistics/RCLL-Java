package com.rcll.llsf_exceptions;

public class UnknownProtocolVersionException extends RuntimeException {
	
	private static final long serialVersionUID = -5191397306313839765L;

	public UnknownProtocolVersionException(String message) {
		super(message);
	}

}
