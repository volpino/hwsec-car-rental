/**
 * 
 */
package rental.applet;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.Applet;
import javacard.framework.ISOException;
import javacard.framework.Util;
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
		new RentalApplet();
	}
	
	RentalApplet() {
		// Create instances of keys.
		cardPrivKey = (ECPrivateKey) KeyBuilder.buildKey(
			KeyBuilder.TYPE_EC_FP_PRIVATE, KeyBuilder.LENGTH_EC_FP_192, false
		);
		cardPubKey = (ECPublicKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_EC_FP_PUBLIC, KeyBuilder.LENGTH_EC_FP_192, false
		);
		
		companyPubKey = (ECPublicKey) KeyBuilder.buildKey(
				KeyBuilder.TYPE_EC_FP_PUBLIC, KeyBuilder.LENGTH_EC_FP_192, false
		);
		
		companySignature = Signature.getInstance(Signature.ALG_ECDSA_SHA, false);
		companySignature.init(companyPubKey, Signature.MODE_VERIFY);
		
		vehicleSignature = Signature.getInstance(Signature.ALG_ECDSA_SHA, false);
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
}