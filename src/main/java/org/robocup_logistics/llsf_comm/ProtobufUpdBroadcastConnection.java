package org.robocup_logistics.llsf_comm;

import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.GeneratedMessageV3;
import lombok.extern.apachecommons.CommonsLog;
import org.robocup_logistics.llsf_encryption.BufferDecryptor;
import org.robocup_logistics.llsf_encryption.BufferEncryptor;
import org.robocup_logistics.llsf_exceptions.UnknownProtocolVersionException;
import org.robocup_logistics.llsf_utils.Key;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.UnresolvedAddressException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * The ProtobufBroadcastPeer provides the possibility to communicate with a refbox using broadcast messages.
 * You can send broadcast messages (UDP) by calling the enqueue method. To receive messages, register a
 * ProtobufMessageHandler, incoming messages will be passed to your handler. To send and receive
 * stream messages (TCP), use the ProtobufClient.
 */
@CommonsLog
public class ProtobufUpdBroadcastConnection implements ProtobufConnection {
	
	private DatagramSocket socket;
	private InetAddress address;
	
	private String hostname;
	private int sendport;
	private int recvport;
	
	private boolean encrypt;
	
	private BufferEncryptor encryptor;
	private BufferDecryptor decryptor;
	
	private Queue<ProtobufMessage> queue1;
	private Queue<ProtobufMessage> queue2;
	private Queue<ProtobufMessage> act_q;
	private Queue<ProtobufMessage> send_q;
	private SendThread send;
	private RecvThread recv;
	private StopThread stop;
	
	private HashMap<Key, GeneratedMessageV3> msgs = new HashMap<>();
	private HashMap<String, Key> classNameToKey = new HashMap<>();

	private ProtobufMessageHandler handler;
	
	/**
	 * Instantiates a new ProtobufBroadcastPeer. This method does not connect (see start).
	 * 
	 * @param hostname
	 *            the IP address of the refbox
	 * @param sendport
	 *            the port to which to send
	 * @param recvport
	 * 			  the port to listen on for incoming messages
	 * @see start()
	 */
	public ProtobufUpdBroadcastConnection(String hostname, int sendport, int recvport) {
		this.hostname = hostname;
		this.sendport = sendport;
		this.recvport = recvport;
		encrypt = false;
	}
	
	/**
	 * Instantiates a new ProtobufBroadcastPeer with a cipher and an encryption key. The cipher must be
	 * one of the values defined in the refbox integration manual in section 2.2.1. Use this constructor if
	 * you want to send and receive encrypted messages. If you set encrypt to false or the cipher to 0,
	 * encryption is disabled. This method does not connect (see start).
	 * 
	 * @param hostname
	 *            the IP address of the refbox
	 * @param sendport
	 *            the port to which to send
	 * @param recvport
	 * 			  the port to listen on for incoming messages
	 * @param encrypt
	 * 			  enables or disables encryption
	 * @param cipher
	 * 			  the cipher as defined in the refbox integration manual in section 2.2.1
	 * @param encryptionKey
	 * 			  the encryption key as String
	 * @see start()
	 */
	public ProtobufUpdBroadcastConnection(String hostname, int sendport, int recvport, boolean encrypt, int cipher, String encryptionKey) {
		this(hostname, sendport, recvport);
		this.encrypt = encrypt;
		
		if (encrypt && cipher != 0) {
			try {
				encryptor = new BufferEncryptor(cipher, encryptionKey);
				decryptor = new BufferDecryptor(encryptionKey);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			}	
		} else {
			this.encrypt = false;
		}
	}
	
	/**
	 * Enables encryption with the given cipher and encryption key. From now on
	 * messages will be sent and received encrypted if you set encrypt to true
	 * and the cipher to a non-zero value.
	 * 
	 * @param encrypt
	 * 			  enables or disables encryption
	 * @param cipher
	 * 			  the cipher as defined in the refbox integration manual in section 2.2.1
	 * @param encryptionKey
	 * 			  the encryption key as String
	 */
	public void setEncrypt(boolean encrypt, int cipher, String encryptionKey) {
		this.encrypt = encrypt;
		
		if (encrypt && cipher != 0) {
			try {
				encryptor = new BufferEncryptor(cipher, encryptionKey);
				decryptor = new BufferDecryptor(encryptionKey);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			}	
		} else {
			this.encrypt = false;
		}
	}

	/**
	 * Opens the socket to be able to send messages and starts listening on the receive port passed
	 * to the constructor.
	 * 
	 * @throws IOException
	 *             Signals that the connection to the refbox cannot be established.
	 */
	public void start(String threadName) throws IOException {
		if (stop != null && stop.isAlive()) {
			try {
				stop.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		ConThread con = new ConThread();
		con.setName(threadName);
		con.start();
		try {
			con.join();
			if (con.getException() == null) {
				
				//Initialize Queues
				queue1 = new LinkedList<ProtobufMessage>();
				queue2 = new LinkedList<ProtobufMessage>();
				act_q = queue1;
				send_q = queue2;
				
				//Start Send and Receive Threads
				send = new SendThread();
				send.start();
				recv = new RecvThread();
				recv.start();
				
			} else {
				if (con.getException() instanceof UnknownHostException) {
					throw new UnknownHostException("Don't know about host " + hostname);
				} else if (con.getException() instanceof IOException) {
					throw new IOException("Couldn't get I/O for the connection to " + hostname + "(" + con.getException().getMessage() + ")");
				} else if (con.getException() instanceof UnresolvedAddressException) {
					throw new UnresolvedAddressException();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		log.info("Started ProtobufBroadcastPeer");
	}
	
	/**
	 * Stops the ProtobufBroadcastPeer and closes the sockets.
	 */
	public void stop() {
		if (stop != null && stop.isAlive()) {
			try {
				stop.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		stop = new StopThread();
		stop.start();
		log.info("Stoped ProtobufBroadcastPeer");
	}
	
	/**
	 * Registers a new ProtobufMessageHandler responsible for handling and deserializing incoming
	 * protobuf messages. Required if you want to access received messages. Only allows one registered
	 * handler at the same time.
	 * 
	 * @param handler
	 *            the ProtobufMessageHandler
	 * @see ProtobufMessageHandler
	 */
	public void register_handler(ProtobufMessageHandler handler) {
		this.handler = handler;
	}
	
	/**
	 * Adds and registers a new protobuf message type. This is required to instantiate the correct
	 * protobuf message object when a message is received from the refbox. For example, if you want
	 * the client to be able to receive and process a GameState message, call 
	 * client.<GameState>add_message(GameState.class).
	 * 
	 * @param <T>
	 *            the type of the protobuf message to register, has to extend from GeneratedMessage 
	 * @param c
	 *            the class object of the same protobuf message
	 */
	@SuppressWarnings("unchecked")
	public <T extends GeneratedMessageV3> void add_message(Class<T> c) {
		try {
			Method m = c.getMethod("getDefaultInstance", (Class<?>[]) null);
			T msg = (T) m.invoke((Object[]) null, (Object[]) null);
			EnumDescriptor desc = msg.getDescriptorForType().findEnumTypeByName("CompType");
			
			int cmp_id = desc.findValueByName("COMP_ID").getNumber();
			int msg_id = desc.findValueByName("MSG_TYPE").getNumber();
			Key key = new Key(cmp_id, msg_id);
			msgs.put(key, msg);
			classNameToKey.put(c.getName(), key);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	private void handle_message(int cmp_id, int msg_id, ByteBuffer in_msg) {
		if (handler != null) {
			for (Map.Entry<Key, GeneratedMessageV3> e: msgs.entrySet()) {
				Key key = e.getKey();
				if (key.cmp_id == cmp_id && key.msg_id == msg_id) {
					handler.handle_message(in_msg, e.getValue());
					break;
				}
			}	
		}
	}

	public void enqueue(GeneratedMessageV3 msg) {
		Key key = classNameToKey.get(msg.getClass().getName());
		this.enqueue(new ProtobufMessage(key.cmp_id, key.msg_id, msg));
	}

	/**
	 * Puts a ProtobufMessage into the send queue in order to be sent out to the refbox.
	 * 
	 * @param msg
	 *            the ProtobufMessage to send
	 * @see ProtobufMessage
	 */
	public void enqueue(ProtobufMessage msg) {
		/*synchronized (act_q) {
			act_q.add(msg);
			try {
			    System.out.print("f");
				act_q.notifyAll();
			} catch(IllegalMonitorStateException e) {}
		}*/
		if (socket != null) {
			byte[] sendData = msg.serialize(encrypt, encryptor).array();
			DatagramPacket send = new DatagramPacket(sendData, sendData.length, address, sendport);
			try {
				socket.send(send);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			log.error("Socket not yet created!");
		}
	}
	
	private class ConThread extends Thread {
		
		private Exception e = null;
		
		public void run() {
			try {
				socket = new DatagramSocket(recvport);
				socket.setBroadcast(true);
				address = InetAddress.getByName(hostname);
	        } catch (UnknownHostException e) {
	        	this.e = e;
	        } catch (IOException e) {
	        	this.e = e;
	        } catch (UnresolvedAddressException e) {
	        	this.e = e;
	        }
		}
		
		public Exception getException() {
			return this.e;
		}
		
	}
	
	private class StopThread extends Thread {
		
		public void run() {
			
			if (send != null) {
				send.terminate();
			}
			if (recv != null) {
				recv.terminate();
			}
			
			if (socket != null) {
				socket.close();
				socket = null;
			}
			
			try {
				if (send != null) {
					send.join();
				}
				if (recv != null) {
					recv.join();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
	}

	private class SendThread extends Thread {
		
		private boolean run = true;

		public void run() {
			log.info("Started SendThread!");
			while (run) {
				synchronized (act_q) {
					if (act_q.isEmpty()) {
						try {
							act_q.wait();
							Queue<ProtobufMessage> help_q = send_q;
							send_q = act_q;
							act_q = help_q;
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}	
					}

				synchronized (send_q) {
					System.out.print("r");
					try {
						while (!send_q.isEmpty()) {
							ProtobufMessage msg = send_q.remove();
							byte[] sendData = msg.serialize(encrypt, encryptor).array();
							DatagramPacket send = new DatagramPacket(sendData, sendData.length, address, sendport);
							if (socket != null) {
								System.out.print(".");
								socket.send(send);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				}
			}
		}

		public void terminate() {
			this.run = false;
			this.interrupt();
			synchronized(act_q) {
				act_q.notifyAll();	
			}
		}
		
	}

	private class RecvThread extends Thread {
		
		private boolean run = true;

		public void run() {
			log.info("Started RecvThread!");
			while (run) {
				try {
					byte[] receiveData = new byte[socket.getReceiveBufferSize()];
					DatagramPacket receive = new DatagramPacket(receiveData, receiveData.length);
					if (socket != null) {
						socket.receive(receive);	
					}
					
					byte[] frame_header = new byte[ProtobufMessage.FRAME_HEADER_SIZE];
					System.arraycopy(receiveData, 0, frame_header, 0, ProtobufMessage.FRAME_HEADER_SIZE);
					ByteBuffer frame_header_buf = ByteBuffer.wrap(frame_header);
					frame_header_buf.order(ByteOrder.BIG_ENDIAN);
					frame_header_buf.rewind();
					
					ProtobufFrameHeader frameHeader = readHeader(frame_header_buf);
					
					int protocolVersion = frameHeader.getProtocolVersion();
					if (protocolVersion != 2) {
						throw new UnknownProtocolVersionException("Protocol version " + protocolVersion + " does not exist and cannot be processed.");
					}
					
					int cipher = frameHeader.getCipher();
					boolean encrypted;
					if (cipher == 0) {
						encrypted = false;
					} else {
						encrypted = true;
					}
					
					int payloadSize = frameHeader.getPayloadSize();
					
					if (payloadSize < 0 || payloadSize > 1000000) {
						continue;
					}
					
					if (encrypted) {
						
						byte[] initializationVector = null;
						byte[] data = null;
						if (cipher == 2 || cipher == 4) { //CBC
							int ivSize = encryptor.getIvSize();
							
							initializationVector = new byte[ivSize];
							System.arraycopy(receiveData, ProtobufMessage.FRAME_HEADER_SIZE, initializationVector, 0, ivSize);
							data = new byte[payloadSize - ivSize];
							System.arraycopy(receiveData, ProtobufMessage.FRAME_HEADER_SIZE + ivSize, data, 0, payloadSize - ivSize);	
						} else if (cipher == 1 || cipher == 3) { //ECB
							data = new byte[payloadSize];
							System.arraycopy(receiveData, ProtobufMessage.FRAME_HEADER_SIZE, data, 0, payloadSize);
						}
						
						try {
							byte[] decryptedData = decryptor.decrypt(cipher, data, initializationVector);
							
							ByteBuffer finalData = ByteBuffer.wrap(decryptedData);
							finalData.order(ByteOrder.BIG_ENDIAN);
							finalData.rewind();
							
							int cmp_id = finalData.getShort();
							int msg_id = finalData.getShort();
							
							ByteBuffer message = ByteBuffer.allocate(decryptedData.length - ProtobufMessage.MESSAGE_HEADER_SIZE);
							message.rewind();
							message.put(finalData);
							
							handle_message(cmp_id, msg_id, (ByteBuffer) message.rewind());
							
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
						
					} else {
						
						byte[] msg_header = new byte[ProtobufMessage.MESSAGE_HEADER_SIZE];
						System.arraycopy(receiveData, ProtobufMessage.FRAME_HEADER_SIZE, msg_header, 0, ProtobufMessage.MESSAGE_HEADER_SIZE);
						ByteBuffer msg_header_buf = ByteBuffer.wrap(msg_header);
						msg_header_buf.order(ByteOrder.BIG_ENDIAN);
						msg_header_buf.rewind();
						
						int cmp_id = msg_header_buf.getShort();
						int msg_id = msg_header_buf.getShort();
						
						byte[] data = new byte[payloadSize - ProtobufMessage.MESSAGE_HEADER_SIZE];
						System.arraycopy(receiveData, ProtobufMessage.FRAME_HEADER_SIZE + ProtobufMessage.MESSAGE_HEADER_SIZE, data, 0, payloadSize - ProtobufMessage.MESSAGE_HEADER_SIZE);
						ByteBuffer data_buf = ByteBuffer.wrap(data);
						
						handle_message(cmp_id, msg_id, (ByteBuffer) data_buf.rewind());
						
					}					
				} catch (ClosedByInterruptException e) {
				} catch (IOException e) {
				}
			}
		}

		public void terminate() {
			this.run = false;
			this.interrupt();
		}
		
		private ProtobufFrameHeader readHeader(ByteBuffer header) {
			ProtobufFrameHeader frameHeader = new ProtobufFrameHeader();
			
			frameHeader.setProtocolVersion((int) header.get());
			frameHeader.setCipher((int) header.get());
			frameHeader.setReserved1((int) header.get());
			frameHeader.setReserved2((int) header.get());
			
			frameHeader.setPayloadSize(header.getInt());
			
			return frameHeader;
		}
		
	}

	private String getName() {
		return this.hostname + ":" + recvport + "- sending on: " + sendport;
	}
	
}
