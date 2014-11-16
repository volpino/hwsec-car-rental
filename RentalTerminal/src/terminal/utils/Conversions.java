package terminal.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

public class Conversions {
	final static int FIELD_SIZE = 21; // Specific for 163 bit curve (163bit = 21 bytes)

	public static byte[] shortToBytes(short s) {
		byte[] res = { (byte) ((s >> 8) & 0xFF), (byte) (s & 0xFF) };
		return res;
	}
	
	public static short bytesToShort(byte[] b) {
		return (short) (((b[0] & 0xFF) << 8) + (b[1] & 0xFF));
	}

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

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static byte[] getChunk(byte[] buf, int base, int index) {
		int length = buf[base] & 0xFF;
		for (int i = 0; i < index; i++) {
			base += length + 1;
			length = buf[base] & 0xFF;
		}
		return Arrays.copyOfRange(buf, base + 1, base + 1 + buf[base]);
	}

	public static long bytesToLong(byte[] bytes) {
		ByteBuffer wrapped = ByteBuffer.wrap(bytes); // big-endian by default
		long num = wrapped.getLong();
		return num;
	}
	
	public static byte[] longToBytes(long x) {
	    ByteBuffer buffer = ByteBuffer.allocate(8);
	    buffer.putLong(x);
	    return buffer.array();
	}
}
