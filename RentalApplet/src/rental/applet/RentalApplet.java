/**
 * 
 */
package rental.applet;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.Applet;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.CryptoException;
import javacard.security.ECKey;
import javacard.security.ECPrivateKey;
import javacard.security.ECPublicKey;
import javacard.security.KeyBuilder;
import javacard.security.RandomData;
import javacard.security.Signature;

/**
 * @author javacard
 *
 */
public class RentalApplet extends Applet {
	public static final byte STATUS_UNINITIALIZED = 0;
	public static final byte STATUS_INITIALIZED = 1;

	private byte status = STATUS_UNINITIALIZED;

	public static final byte CLA_ISSUE = (byte) 0xB0;
	public static final byte CMD_CARDID = (byte) 0x00;
	public static final byte CMD_CARDKEYS = (byte) 0x01;
	public static final byte CMD_COMPANYPUB = (byte) 0x02;
	public static final byte CMD_RANDSEED = (byte) 0x03;
	
	public static final byte CLA_RECEPTION = (byte) 0xB1;
	public static final byte CMD_REC_INIT = (byte) 0x00;
	public static final byte CMD_REC_COMMAND = (byte) 0x01;

	private short cardID = 0;
	private short vehicleID = 1337;
	private short kilometers = 9999;

	private RandomData random;
	
	// Curve EC_F2M_163 parameters
	static final byte[] CURVE_A = {7, 37, 70, -75, 67, 82, 52, -92, 34, -32, 120, -106, 117, -12, 50, -56, -108, 53, -34, 82, 66};
	static final byte[] CURVE_B = {0, -55, 81, 125, 6, -43, 36, 13, 60, -1, 56, -57, 75, 32, -74, -51, 77, 111, -99, -44, -39};
	static final byte[] CURVE_R = {4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -26, 15, -56, -126, 28, -57, 77, -82, -81, -63};
	static final short[] CURVE_P = {8, 2, 1};
	static final byte[] CURVE_G = {4, 7, -81, 105, -104, -107, 70, 16, 61, 121, 50, -97, -52, 61, 116, -120, 15, 51, -69, -24, 3, -53, 1, -20, 35, 33, 27, 89, 102, -83, -22, 29, 63, -121, -9, -22, 88, 72, -82, -16, -73, -54, -97};

	// Ram-stored arrays
	short[] offset;
	byte[] nonce;
	byte[] tmp;
	
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
		// Create instances of transient arrays
		offset = JCSystem.makeTransientShortArray((short) 2, JCSystem.CLEAR_ON_RESET);
		nonce = JCSystem.makeTransientByteArray((short) 8, JCSystem.CLEAR_ON_RESET);
		tmp = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);

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
		
		initKey(cardPrivKey);
		initKey(cardPubKey);
		initKey(companyPubKey);
		
		companySignature = Signature.getInstance(Signature.ALG_ECDSA_SHA, false);
		vehicleSignature = Signature.getInstance(Signature.ALG_ECDSA_SHA, false);
		cardSignature = Signature.getInstance(Signature.ALG_ECDSA_SHA, false);
		
		random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		if(status == STATUS_UNINITIALIZED) {
			if (buf[ISO7816.OFFSET_CLA] == CLA_ISSUE) {
				switch (buf[ISO7816.OFFSET_INS]) {
				case CMD_CARDID:
					cardID = Util.getShort(buf, ISO7816.OFFSET_CDATA);
					break;
				case CMD_CARDKEYS:
					findOffset(buf, (short) ISO7816.OFFSET_CDATA, (short) 0);
					cardPubKey.setW(buf, offset[0], offset[1]);
					findOffset(buf, (short) ISO7816.OFFSET_CDATA, (short) 1);
					cardPrivKey.setS(buf, offset[0], offset[1]);
					
					cardSignature.init(cardPrivKey, Signature.MODE_SIGN);
					break;
				case CMD_COMPANYPUB:
					findOffset(buf, (short) ISO7816.OFFSET_CDATA, (short) 0);
					companyPubKey.setW(buf, offset[0], offset[1]);
					
					companySignature.init(companyPubKey, Signature.MODE_VERIFY);
					break;
				case CMD_RANDSEED:
					random.setSeed(buf, (short) 0, (short) 8);
					status = STATUS_INITIALIZED;
					break;
				default:
					// good practice: If you don't know the INStruction, say so:
					ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
				}
			}
			else {
				ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
			}
		}
		else {
			if (buf[ISO7816.OFFSET_CLA] == CLA_RECEPTION) {
				switch (buf[ISO7816.OFFSET_INS]) {
				case CMD_REC_INIT:
					// save the nonce in tmp
					Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, tmp, (short)0, (short)8);
					// Copy cardID and new nonce to the response buffer
					Util.setShort(buf, (short)0, cardID);
					random.generateData(buf, (short) 2, (short)8);
					// sign the nonce and put it in the response buffer
					short sigLen = cardSignature.sign(tmp, (short)0, (short)8, buf, (short)(2+8));
					short totLen = (short) (sigLen + 2 + 8);
					
					short le = apdu.setOutgoing();
					if (le < totLen) {
						ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
					}
					apdu.setOutgoingLength(totLen);					 
					apdu.sendBytes((short)0, totLen);
					break;
				default:
					ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
				}
			}
			else {
				ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
			}
		}
	}
	
	void findOffset(byte[] buf, short base, short index) {
		short length = (short) (buf[base] & 0xFF);
		for (short i=0; i<index; i++) {
			base += length + 1;
		}
		offset[0] = (short) (base + 1);
		offset[1] = (short) (buf[base] & 0xFF);
	}
	
	void initKey(ECKey key) {
		key.setA(CURVE_A, (short)0, (short) CURVE_A.length);
		key.setB(CURVE_B, (short)0, (short) CURVE_B.length);
		key.setR(CURVE_R, (short)0, (short) CURVE_R.length);
		key.setFieldF2M(CURVE_P[0],CURVE_P[1],CURVE_P[2]);
		key.setG(CURVE_G, (short)0, (short) CURVE_G.length);
	}
}