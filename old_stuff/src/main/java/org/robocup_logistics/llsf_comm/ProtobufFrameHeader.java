package org.robocup_logistics.llsf_comm;

/**
 * This class represents the frame header of messages. It is used internally when deserializing an
 * incoming message.
 */
public class ProtobufFrameHeader {
	
	private int protocolVersion;
	private int cipher;
	private int reserved1;
	private int reserved2;
	
	private int payloadSize;
	
	public ProtobufFrameHeader() {
		protocolVersion = 2;
		cipher = 0;
		reserved1 = 0;
		reserved2 = 0;
		
		payloadSize = 0;
	}

	public int getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(int protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public int getCipher() {
		return cipher;
	}

	public void setCipher(int cipher) {
		this.cipher = cipher;
	}

	public int getReserved1() {
		return reserved1;
	}

	public void setReserved1(int reserved1) {
		this.reserved1 = reserved1;
	}

	public int getReserved2() {
		return reserved2;
	}

	public void setReserved2(int reserved2) {
		this.reserved2 = reserved2;
	}

	public int getPayloadSize() {
		return payloadSize;
	}

	public void setPayloadSize(int payloadSize) {
		this.payloadSize = payloadSize;
	}

}
