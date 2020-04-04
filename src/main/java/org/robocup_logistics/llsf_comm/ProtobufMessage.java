package org.robocup_logistics.llsf_comm;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessageV3;
import org.robocup_logistics.llsf_encryption.BufferEncryptor;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * This class is a wrapper for protobuf messages. It is used to identify incoming messages
 * via the component and message IDs. To send a protobuf message, create an instance of
 * this class with the same component ID and message ID as defined in the corresponding .proto file
 * of the protobuf message you want to send. Then use the setter to add an instance of the actual
 * protobuf message. You can find a detailed example in the tutorial.
 */
public class ProtobufMessage {
	
	protected int protocolVersion = 2;
	protected int reserved1 = 0;
	protected int reserved2 = 0;

	protected int payload_size;
	protected int cmp_id;
	protected int msg_id;
	protected byte[] msg;

	public final static int FRAME_HEADER_SIZE = 8;
	public final static int MESSAGE_HEADER_SIZE = 4;
	
	/**
	 * Instantiates a new ProtobufMessage. Pass the same component ID and message ID as defined in
	 * the corresponding .proto file of the protobuf message you want to send.
	 * 
	 * @param cmp_id
	 *            the component ID defined in the .proto file
	 * @param msg_id
	 *            the message ID defined in the .proto file
	 */
	public ProtobufMessage(int cmp_id, int msg_id) {
		this.payload_size = 0;
		this.cmp_id = cmp_id;
		this.msg_id = msg_id;
		this.msg = null;
	}
	
	/**
	 * Instantiates a new ProtobufMessage with an instance of the actual protobuf message. 
	 * Pass the same component ID and message ID as defined in the corresponding .proto file
	 * of the protobuf message you want to send. You can find a detailed example in the tutorial.
	 * 
	 * @param cmp_id
	 *            the component ID defined in the .proto file
	 * @param msg_id
	 *            the message ID defined in the .proto file
	 * @param gmsg
	 *            the instance of the actual protobuf message
	 */
	public ProtobufMessage(int cmp_id, int msg_id, GeneratedMessageV3 gmsg) {
		this(cmp_id, msg_id);
		this.set_message(gmsg);
	}
	
	/**
	 * Gets the component ID of the message.
	 * 
	 * @return the component ID
	 */
	public int get_component_id() {
		return this.cmp_id;
	}
	
	/**
	 * Gets the message ID of the message.
	 * 
	 * @return the message ID
	 */
	public int get_message_id() {
		return this.msg_id;
	}
	
	/**
	 * Gets the size of the message. Will return 0 if no message has been set yet.
	 * 
	 * @return the size of the message
	 */
	public int get_size() {
		return this.payload_size;
	}
	
	/**
	 * Gets the protobuf message as a byte array.
	 * 
	 * @return the protobuf message
	 */
	public byte[] get_message() {
		return this.msg;
	}
	
	/**
	 * Sets the component ID of the message. Pass the same component ID as defined in the
	 * corresponding .proto file of the protobuf message you want to send.
	 * 
	 * @param cmp_id
	 *            the component ID
	 */
	public void set_component_id(int cmp_id) {
		this.cmp_id = cmp_id;
	}
	
	/**
	 * Sets the message ID of the message. Pass the same message ID as defined in the
	 * corresponding .proto file of the protobuf message you want to send.
	 * 
	 * @param msg_id
	 *            the message ID
	 */
	public void set_message_id(int msg_id) {
		this.msg_id = msg_id;
	}
	
	/**
	 * Sets the protobuf message. Pass an instance of the actual protobuf message you want to send.
	 * 
	 * @param gmsg
	 *            the protobuf message, has to extend from GeneratedMessage
	 */
	public void set_message(GeneratedMessageV3 gmsg) {
		int cmp = (int) Math.pow(2, 31) - 1;
		byte[] msg = gmsg.toByteArray();
		if (msg.length > cmp) {
			//throw new FawkesNetworkMessageTooBigException("Network Message size too big");
		} else {
			this.payload_size = msg.length + MESSAGE_HEADER_SIZE;
			this.msg = msg;
		}
	}
	
	/**
	 * Serializes the ProtobufMessage. This method will automatically be called by the
	 * ProtobufClient/ProtobufBroadcastPeer when the message is to be sent out.
	 * 
	 * @return the ByteBuffer containing the ProtobufMessage
	 */
	public ByteBuffer serialize(boolean encrypt, BufferEncryptor encryptor) {
		
		int completeSize = 0;
		int cipher = 0;
		byte[] iv = null;
		
		if (encrypt) {
			cipher = encryptor.getCipher();
		}
		
		byte[] encryptedData = null;
		if (encrypt) {
			ByteBuffer toEncrypt = ByteBuffer.allocate(payload_size).order(ByteOrder.BIG_ENDIAN);
			toEncrypt.putShort((short) cmp_id).putShort((short) msg_id);
			toEncrypt.put(msg);
			
			try {
				encryptedData = encryptor.encrypt(toEncrypt);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			}
			
			if (cipher == 2 || cipher == 4) { //CBC
				iv = encryptor.getIv();
				completeSize = FRAME_HEADER_SIZE + encryptor.getIvSize() + encryptedData.length;
			} else if (cipher == 1 || cipher == 3) { //ECB
				completeSize = FRAME_HEADER_SIZE + encryptedData.length;
			}
			payload_size = completeSize - FRAME_HEADER_SIZE;
			
		} else {
			completeSize = FRAME_HEADER_SIZE + payload_size;
		}
		
		ByteBuffer buf = ByteBuffer.allocate(completeSize).order(ByteOrder.BIG_ENDIAN);
		if (this.cmp_id == -1 || this.msg_id == -1) {
			return null;
			//throw new FawkesNetworkMessageUnknownIDException("Unknown component or message ID");
		} else {
			buf.put((byte) protocolVersion).put((byte) cipher).put((byte) reserved1).put((byte) reserved2);

			buf.putInt(payload_size);
			
			if (encrypt) {
				if (cipher == 2 || cipher == 4) { //CBC
					buf.put(iv);	
				}
				buf.put(encryptedData);
			} else {
				buf.putShort((short) this.cmp_id).putShort((short) this.msg_id);
				buf.put(this.msg);
			}
			
			if (cipher == 2 || cipher == 4) { //CBC
				encryptor.createNextIv();
			}
			
			return (ByteBuffer) buf.rewind();	
		}
	}
	

}
