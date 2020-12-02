package org.robocup_logistics.llsf_comm;

import com.google.protobuf.GeneratedMessageV3;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The Interface ProtobufMessageHandler is used to handle received protobuf messages. Implement
 * this interface if you want to be able to access the information in the retrieved messages.
 * The ProtobufClient/ProtobufBroadcastPeer automatically passes incoming messages to your handler
 * if you have registered it.
 * 
 * @see ProtobufTcpConnection#register_handler(ProtobufMessageHandler)
 * @see ProtobufUpdBroadcastConnection#register_handler(ProtobufMessageHandler handler)
 */
public interface ProtobufMessageHandler {
	
	/**
	 * This method is called by the ProtobufClient/ProtobufBroadcastPeer. The GeneratedMessage
	 * passed to it is an instance of the same type as the protobuf message you received. It is
	 * used to identify the type of the protobuf message. The actual model is contained in the
	 * ByteBuffer. You can read the tutorial to find out how to handle incoming messages correctly.
	 *  
	 * @param in_msg
	 *            the ByteBuffer containing the actual model
	 * @param msg
	 *            the instance of the same type as the protobuf message you received
	 */
	public void handle_message(ByteBuffer in_msg, GeneratedMessageV3 msg);
	public void connection_lost(IOException e);
	public void timeout();

}
