/**
 * 
 */
package rental.applet;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.Applet;
import javacard.framework.ISOException;
import javacard.framework.Util;
import javacard.security.CryptoException;
import javacard.security.ECPrivateKey;
import javacard.security.ECPublicKey;
import javacard.security.KeyBuilder;
import javacard.security.Signature;

/**
 * @author javacard
 *
 */
public class RentalApplet extends Applet {
	public static final byte STATUS_UNINITIALIZED = 0;
	public static final byte STATUS_INITIALIZED   = 1;

	private byte status = STATUS_UNINITIALIZED;

	public static final byte CLA_ISSUE           = (byte) 0xB0;
	public static final byte CLA_AUTH            = (byte) 0xB1;

	public static final byte CMD_INIT            = 0x00;
	public static final byte CMD_RESET           = 0x41;
	
	public static final byte CMD_READ_CARD_ID    = 0x50;
	public static final byte CMD_READ_VEHICLE_ID = 0x51;
	public static final byte CMD_READ_KILOMETERS = 0x52;
	public static final byte CMD_ADD_KILOMETERS  = 0x53;

	public static final byte CMD_KEY_TEST        = 0x60;
	public static final byte CMD_VERIFY_TEST     = 0x61;
	
	private short cardID     = 0;
	private short vehicleID  = 1337;
	private short kilometers = 9999;
	
	ECPrivateKey cardPrivKey;
	ECPublicKey cardPubKey;
	
	ECPublicKey companyPubKey;
	
	Signature companySignature;
	Signature vehicleSignature;
	Signature cardSignature;
	
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new RentalApplet().register(bArray,
				(short) (bOffset + 1), bArray[bOffset]);
	}
	
	RentalApplet() {
		// Create instances of keys.
		
		cardPrivKey = (ECPrivateKey) KeyBuilder.buildKey(
			KeyBuilder.TYPE_EC_F2M_PRIVATE, KeyBuilder.LENGTH_EC_F2M_163, false
		);
		
		cardPubKey = (ECPublicKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_EC_F2M_PUBLIC, KeyBuilder.LENGTH_EC_F2M_163, false
		);
		companyPubKey = (ECPublicKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_EC_F2M_PUBLIC, KeyBuilder.LENGTH_EC_F2M_163, false
		);
		
		companySignature = Signature.getInstance(Signature.ALG_ECDSA_SHA, false);
		/*
		vehicleSignature = Signature.getInstance(Signature.ALG_ECDSA_SHA, false);
		*/
		cardSignature = Signature.getInstance(Signature.ALG_ECDSA_SHA, false);
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		if(status == STATUS_UNINITIALIZED && buf[ISO7816.OFFSET_CLA] == CLA_ISSUE) {
			if(buf[ISO7816.OFFSET_INS] == CMD_INIT) {
				cardID = Util.getShort(buf, ISO7816.OFFSET_CDATA);
				status = STATUS_INITIALIZED;
			} else {
				// good practice: If you don't know the INStruction, say so:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
		} else {
			switch (buf[ISO7816.OFFSET_INS]) {
			case CMD_KEY_TEST:
					try {
						final byte[] a = {7, 37, 70, -75, 67, 82, 52, -92, 34, -32, 120, -106, 117, -12, 50, -56, -108, 53, -34, 82, 66};
						cardPrivKey.setA(a, (short)0, (short) a.length);
						cardPubKey.setA(a, (short)0, (short) a.length);
					} catch(Exception e) {
						ISOException.throwIt((short) 0x6001);					
					}
					try {
						final byte[] b = {0, -55, 81, 125, 6, -43, 36, 13, 60, -1, 56, -57, 75, 32, -74, -51, 77, 111, -99, -44, -39};
						cardPrivKey.setB(b, (short)0, (short) b.length);
						cardPubKey.setB(b, (short)0, (short) b.length);
					} catch(Exception e) {
						ISOException.throwIt((short) 0x6002);					
					}
					try {
						final byte[] r = {4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -26, 15, -56, -126, 28, -57, 77, -82, -81, -63};
						cardPrivKey.setR(r, (short)0, (short) r.length);
						cardPubKey.setR(r, (short)0, (short) r.length);
					} catch(Exception e) {
						ISOException.throwIt((short) 0x6003);					
					}
					try {
						final short[] p = {8,2,1};
						cardPrivKey.setFieldF2M(p[0],p[1],p[2]);
						cardPubKey.setFieldF2M(p[0],p[1],p[2]);
					} catch(Exception e) {
						ISOException.throwIt((short) 0x6004);					
					}
					try {
						final byte[] g = {4,  7, -81, 105, -104, -107, 70, 16, 61, 121, 50, -97, -52, 61, 116, -120, 15, 51, -69, -24, 3, -53, 1, -20, 35, 33, 27, 89, 102, -83, -22, 29, 63, -121, -9, -22, 88, 72, -82, -16, -73, -54, -97};
						cardPrivKey.setG(g, (short)0, (short) g.length);
						cardPubKey.setG(g, (short)0, (short) g.length);
					} catch(Exception e) {
						ISOException.throwIt((short) 0x6005);					
					}
					
					try {
						final byte[] w = {4,  3, -58, 83, -110, -109, -15, 35, -112, -57, -96, -82, 47, -69, 1, 58, 112, 86, 35, -14, 127, 16, 1, -45, -112, 113, 22, 3, -12, -112, 94, -22, -14, 105, -42, -33, 4, -128, 24, 114, 82, 73, 45 };
						cardPubKey.setW(w, (short)0, (short) w.length);
					} catch(Exception e) {
						ISOException.throwIt((short) 0x6006);					
					}
					
					try {
						final byte[] s = {1, 66, -100, -58, 22, -15, 100, -9, -12, -18, -120, -92, 45, 49, 73, -38, -46, 41, 48, -22, -82};
						cardPrivKey.setS(s, (short)0, (short) s.length);
					} catch(Exception e) {
						ISOException.throwIt((short) 0x6005);					
					}
					companySignature.init(cardPubKey, Signature.MODE_VERIFY);
					cardSignature.init(cardPrivKey, Signature.MODE_SIGN);
					//ISOException.throwIt((short) 0x9999);
					break;
			case CMD_VERIFY_TEST:
					/*
				    short inLength = Util.getShort(buf, ISO7816.OFFSET_CDATA);
					short inOffset = ISO7816.OFFSET_CDATA+2;
					short sigLength = (short)(buf.length - inOffset - inLength);
					short sigOffset = (short) (inOffset + inLength);
					*/
					boolean res = false;
					byte[] data = {72, 101, 121, 74, 117, 115, 116, 84, 114, 121, 73, 116};
					byte[] sig = {48, 45, 2, 20, 12, -65, -15, -86, -69, -127, -120, -52, 79, 27, -84, -3, -34, 81, -105, 86, -62, 74, -106, 104, 2, 21, 1, -88, 84, -98, 103, 30, -20, 24, -101, -82, 55, 122, 16, -39, -68, -86, 57, -24, -70, 62, 123};
					short siglen = 0;
						//res = companySignature.verify(buf, inOffset, inLength, buf, sigOffset, sigLength);
						//res = companySignature.verify(data, (short)0, (short)data.length, sig, (short)0, (short)sig.length);
						try {
						siglen = cardSignature.sign(data, (short)0, (short)data.length, buf, (short) 2);
						} catch (Exception e) {
							ISOException.throwIt((short)0x6001);
						}
						try {
							res = companySignature.verify(data, (short)0, (short)data.length, buf, (short) 2, siglen);
						} catch(Exception e) {
							ISOException.throwIt((short)0x6002);
						}

						if(!res) {
							ISOException.throwIt((short)0x6004);
						}
						Util.setShort(buf, (short) 0, siglen);
						short outlen = apdu.setOutgoing();
						short myoutlen = (short) (2+siglen);
						if(myoutlen > outlen) {
							ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);	
						}
						apdu.setOutgoingLength(myoutlen);
						apdu.sendBytes((short) 0, myoutlen);
					/*
					if (!res) {
						ISOException.throwIt((short)0x6666);
					}*/
						ISOException.throwIt((short)0x9999);
				break;
			case CMD_RESET:
				status = STATUS_UNINITIALIZED;
				cardID = 0;
				kilometers = 0;
				break;
			case CMD_READ_CARD_ID:
				Util.setShort(buf, (short) 0, cardID);
				apdu.setOutgoingAndSend((short) 0, (short) 2);
				break;
			case CMD_READ_VEHICLE_ID:
				Util.setShort(buf, (short) 0, vehicleID);
				apdu.setOutgoingAndSend((short) 0, (short) 2);
				break;
			case CMD_READ_KILOMETERS:
				Util.setShort(buf, (short) 0, kilometers);
				apdu.setOutgoingAndSend((short) 0, (short) 2);
				break;
			default:
				// good practice: If you don't know the INStruction, say so:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
		}
	}
	/*
	boolean verifyCompany(byte[] data) {
		// TODO: Define some header for messages that contains length
		return companySignature.verify(
			data,  // buffer with data to verify
			(short) 0,  // data to verify offset (start)
			(short) 1,  // length of data to verify
			data,  // signature buffer
			(short) 0,  // signature offset (start)
			(short) 0 // length of the signature
		);
	}
	*/
}