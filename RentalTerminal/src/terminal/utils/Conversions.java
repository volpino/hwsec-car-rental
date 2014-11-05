package terminal.utils;

public class Conversions {
	public static byte[] short2bytes(short s) {
		byte[] res = {(byte)((s >> 8) & 0xff), (byte)(s&0xff) };
		return res;
	}
}
