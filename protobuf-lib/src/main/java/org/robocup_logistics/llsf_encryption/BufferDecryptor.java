/*
 *
 * Copyright (c) 2017, Graz Robust and Intelligent Production System (grips)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.robocup_logistics.llsf_encryption;

import org.robocup_logistics.llsf_exceptions.UnknownEncryptionMethodException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The BufferDecryptor is responsible for decrypting incoming messages.
 */
public class BufferDecryptor {
	
	private byte[] keyBytes;
	
	private static String ALGORITHM;
	private static int KEY_SIZE_BITS;
	
	private byte[][] keyAndIV;
	private SecretKeySpec keySpec;
	
	/**
	 * Instantiates a new BufferDecryptor with an encryption key.
	 * 
	 * @param key The encryption key as String
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	public BufferDecryptor(String key) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException {
		keyBytes = key.getBytes("UTF-8");
	}
	
	/**
	 * Decrypts an incoming message. This method is called by the {@link ProtobufBroadcastPeer}.
	 * 
	 * @param cipher The cipher as defined in the refbox integration manual in section 2.2.1
	 * @param toDecrypt Encrypted model as byte array
	 * @param iv Initialization vector from the incoming message (pass null if you use ECB)
	 * @return Decrypted model as byte array
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public byte[] decrypt(int cipher, byte[] toDecrypt, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		if (cipher == 1) {
			ALGORITHM = "AES/ECB/PKCS5Padding";
			KEY_SIZE_BITS = 128;
		} else if (cipher == 2) {
			ALGORITHM = "AES/CBC/PKCS5Padding";
			KEY_SIZE_BITS = 128;
		} else if (cipher == 3) {
			ALGORITHM = "AES/ECB/PKCS5Padding";
			KEY_SIZE_BITS = 256;
		} else if (cipher == 4) {
			ALGORITHM = "AES/CBC/PKCS5Padding";
			KEY_SIZE_BITS = 256;
		} else {
			throw new UnknownEncryptionMethodException("The encryption method related to cipher " + cipher + " is unknown.");
		}
		
		Cipher c = Cipher.getInstance(ALGORITHM);
		MessageDigest sha = MessageDigest.getInstance("SHA-256");
		
		keyAndIV = KeyConverter.EVP_BytesToKey(KEY_SIZE_BITS / Byte.SIZE, c.getBlockSize(), sha, null, keyBytes, 8);
        keySpec = new SecretKeySpec(keyAndIV[0], "AES");

        if (cipher == 1 || cipher == 3) { //ECB
        	c.init(Cipher.DECRYPT_MODE, keySpec);
		} else if (cipher == 2 || cipher == 4) { //CBC
			c.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
		}
		
		byte[] decryptedData = c.doFinal(toDecrypt);
		
		return decryptedData;
	}
	
}
