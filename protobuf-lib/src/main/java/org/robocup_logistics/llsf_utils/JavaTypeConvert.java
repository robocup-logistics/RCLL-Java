package org.robocup_logistics.llsf_utils;

import java.nio.ByteBuffer;

public class JavaTypeConvert {
	
	public static short getUnsignedInt8(ByteBuffer data) {
		byte bytes = data.get();
		int ints = 0xFF & bytes;
		short n = (short) ints;
		return n;
	}

	public static int getUnsignedInt16(ByteBuffer data) {
		byte[] bytes = new byte[2];
		for (int i = 1; i >= 0; i--) {
			bytes[i] = data.get();
		}
		int[] ints = new int[2];
		for (int i = 0; i < 2; i++) {
			ints[i] = 0xFF & bytes[i];
		}
		int n = ((int) (ints[0] << 8
					| ints[1]))
					& 0xFFFF;			//FIXED! 0xFF is too less for uint16_t!
		return n;
	}
	
	public static int getUnsignedInt16_BE(ByteBuffer data) {
		byte[] bytes = new byte[2];
		for (int i = 0; i <= 1; i++) {
			bytes[i] = data.get();
		}
		int[] ints = new int[2];
		for (int i = 0; i < 2; i++) {
			ints[i] = 0xFF & bytes[i];
		}
		int n = ((int) (ints[0] << 8
					| ints[1]))
					& 0xFFFF;			//FIXED! 0xFF is too less for uint16_t!
		return n;
	}

	public static long getUnsignedInt32(ByteBuffer data) {
		byte[] bytes = new byte[4];
		for (int i = 3; i >= 0; i--) {
			bytes[i] = data.get();
		}
		int[] ints = new int[4];
		for (int i = 0; i < 4; i++) {
			ints[i] = 0xFF & bytes[i];
		}
		long n = ((long) (ints[0] << 24
					| ints[1] << 16
					| ints[2] << 8
					| ints[3]))
					& 0xFFFFFFFFL;
		return n;
	}
	
	public static long getUnsignedInt32_BE(ByteBuffer data) {
		byte[] bytes = new byte[4];
		for (int i = 0; i <= 3; i++) {
			bytes[i] = data.get();
		}
		int[] ints = new int[4];
		for (int i = 0; i < 4; i++) {
			ints[i] = 0xFF & bytes[i];
		}
		long n = ((long) (ints[0] << 24
					| ints[1] << 16
					| ints[2] << 8
					| ints[3]))
					& 0xFFFFFFFFL;
		return n;
	}

	public static String getString(ByteBuffer data, int length) {
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++) {
			bytes[i] = data.get();
		}
		String value = new String(bytes);
		return value;
	}

	public static boolean getBool(ByteBuffer data) {
		byte b = data.get();
		if ((int) b == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public static void putBool(ByteBuffer data, boolean b) {
		if (b) {
			data.put((byte) 42);
		} else {
			data.put((byte) 0);
		}
	}

}
