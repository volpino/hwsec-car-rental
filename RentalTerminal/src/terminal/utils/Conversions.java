package terminal.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

/**
 * Collection of helpers for various conversions
 * 
 * @author Leon Schoorl
 * @author Federico Scrinzi
 *
 */
public class Conversions {
	final static int FIELD_SIZE = 21; // Specific for 163 bit curve (163bit = 21 bytes)

	/**
	 * Converts a short to a byte array of length 2 (big-endian)
	 * 
	 * @param s short to convert
	 * @return resulting byte array
	 */
	public static byte[] shortToBytes(short s) {
		byte[] res = { (byte) ((s >> 8) & 0xFF), (byte) (s & 0xFF) };
		return res;
	}
	
	/**
	 * Convert a byte array of length 2 to a short
	 * 
	 * @param b the byte array to convert
	 * @return the resulting short
	 */
	public static short bytesToShort(byte[] b) {
		return (short) (((b[0] & 0xFF) << 8) + (b[1] & 0xFF));
	}

	/**
	 * Encodes an ECC public key into a byte array using ASN.1 X9.62 format
	 * 
	 * @param pub public key to encode
	 * @return the resulting encoded public key in a byte array
	 */
	public static byte[] encodePubKey(ECPublicKey pub) {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		byte[] x = pub.getW().getAffineX().toByteArray();
		byte[] y = pub.getW().getAffineY().toByteArray();
		try {
			data.write(new byte[] { 0x04 }); // non compressed X9.62 encoding
			for (int i = 0; i < (FIELD_SIZE - x.length); i++) {
				data.write(0); // pad with zeros
			}
			data.write(x);
			for (int i = 0; i < (FIELD_SIZE - y.length); i++) {
				data.write(0); // pad with zeros
			}
			data.write(y);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data.toByteArray();
	}

	/**
	 * Left pads a byte array to reach the field size
	 * 
	 * @param input the byte array to pad
	 * @return the padded byte array
	 */
	public static byte[] padToFieldSize(byte[] input) {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		try {
			for (int i = 0; i < (FIELD_SIZE - input.length); i++) {
				data.write(0); // pad with zeros
			}
			data.write(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data.toByteArray();
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	/**
	 * Converts a byte array to a string using hex format
	 * 
	 * @param bytes
	 * @return string with bytes encoded in hex format
	 */
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * This method allows to extract a chunk from a byte array containing multiple chunks.
	 * The byte array format is of the kind: |L1|P1|L2|P2|L3|P3|...
	 * where Li is the length of the following payload and Pi is the actual payload.
	 * Li is always one byte while Pi can have variable length (specified by Li)
	 * 
	 * @param buf input buffer
	 * @param base offset in the input buffer where the payloads start
	 * @param index the index of the payload we want to get (starting from 0)
	 * @return the resulting chunk
	 */
	public static byte[] getChunk(byte[] buf, int base, int index) {
		int length = buf[base] & 0xFF;
		for (int i = 0; i < index; i++) {
			base += length + 1;
			length = buf[base] & 0xFF;
		}
		return Arrays.copyOfRange(buf, base + 1, base + 1 + buf[base]);
	}

	/**
	 * Converts a byte array of length 8 (big-endian) to a long
	 * 
	 * @param bytes the byte array to convert
	 * @return the resulting long
	 */
	public static long bytesToLong(byte[] bytes) {
		ByteBuffer wrapped = ByteBuffer.wrap(bytes); // big-endian by default
		long num = wrapped.getLong();
		return num;
	}
	
	/**
	 * Converts a long to a byte array of length 8 (big-endian)
	 * 
	 * @param x the long to convert
	 * @return the resulting byte array
	 */
	public static byte[] longToBytes(long x) {
	    ByteBuffer buffer = ByteBuffer.allocate(8);
	    buffer.putLong(x);
	    return buffer.array();
	}
}
