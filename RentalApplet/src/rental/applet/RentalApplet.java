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
	public static final byte CMD_REC_GET_KM = (byte) 0x01;
	public static final byte CMD_REC_RESET_KM = (byte) 0x02;
	public static final byte CMD_REC_CHECK_INUSE = (byte) 0x03;
	public static final byte CMD_REC_ADD_CERT = (byte) 0x04;
	public static final byte CMD_REC_DEL_CERT = (byte) 0x05;
	
	public static final byte CLA_VEHICLE = (byte) 0xB2;
	public static final byte CMD_VEH_INIT = (byte) 0x00;
	public static final byte CMD_VEH_AUTHCARD = (byte) 0x01;
	public static final byte CMD_VEH_AUTHVEHICLE = (byte) 0x02;
	public static final byte CMD_VEH_SAVEKM = (byte) 0x03;
	
	public static final short NONCE_LENGTH = 8;
	public static final short KM_LENGTH = 8;
	public static final short COUNTER_LENGTH = 8;

	private short cardID = 0;
	private byte[] kilometers;
	private byte[] certCounter;
	private boolean inUse = false; 
	private boolean isAssociated = false;
	private boolean[] receptionInitialized;
	private byte[] vehicleInitialized;

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
	byte[] tmp1;
	byte[] tmp2;
	byte[] tmp3;
	
	byte[] vehicleCert;
	short vehicleCertLength;
	
	ECPrivateKey cardPrivKey;
	ECPublicKey cardPubKey;
	
	ECPublicKey companyPubKey;
	ECPublicKey vehiclePubKey;
	
	Signature companySignature;
	Signature vehicleSignature;
	Signature cardSignature;
	
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
		new RentalApplet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}
	
	RentalApplet() {
		// Create instances of transient arrays
		offset = JCSystem.makeTransientShortArray((short) 2, JCSystem.CLEAR_ON_RESET);
		nonce = JCSystem.makeTransientByteArray((short) NONCE_LENGTH, JCSystem.CLEAR_ON_RESET);
		tmp1 = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
		tmp2 = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
		tmp3 = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
		receptionInitialized = JCSystem.makeTransientBooleanArray((short) 1, JCSystem.CLEAR_ON_RESET);
		vehicleInitialized = JCSystem.makeTransientByteArray((short) 1, JCSystem.CLEAR_ON_RESET);
		
		// vehicle certificate array
		vehicleCert = new byte[64];
		
		certCounter = new byte[COUNTER_LENGTH];
		kilometers = new byte[KM_LENGTH];
		
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
		vehiclePubKey = (ECPublicKey) KeyBuilder.buildKey(
			KeyBuilder.TYPE_EC_F2M_PUBLIC, KeyBuilder.LENGTH_EC_F2M_163, false
		);
		
		initKey(cardPrivKey);
		initKey(cardPubKey);
		initKey(companyPubKey);
		initKey(vehiclePubKey);
		
		companySignature = Signature.getInstance(Signature.ALG_ECDSA_SHA, false);
		cardSignature = Signature.getInstance(Signature.ALG_ECDSA_SHA, false);
		vehicleSignature = Signature.getInstance(Signature.ALG_ECDSA_SHA, false);
		
		random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
	}

	public void process(APDU apdu) {
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		if(status == STATUS_UNINITIALIZED) {
			// Issuing commands
			if (buf[ISO7816.OFFSET_CLA] == CLA_ISSUE) {
				switch (buf[ISO7816.OFFSET_INS]) {
				case CMD_CARDID:  // set the card id
					cardID = Util.getShort(buf, ISO7816.OFFSET_CDATA);
					Util.setShort(buf, (short)0, cardID);
					break;
				case CMD_CARDKEYS:  // set the card keypair
					findOffset(buf, (short) ISO7816.OFFSET_CDATA, (short) 0);
					cardPubKey.setW(buf, offset[0], offset[1]);
					findOffset(buf, (short) ISO7816.OFFSET_CDATA, (short) 1);
					cardPrivKey.setS(buf, offset[0], offset[1]);
					
					cardSignature.init(cardPrivKey, Signature.MODE_SIGN);
					break;
				case CMD_COMPANYPUB:  // set the company public key
					findOffset(buf, (short) ISO7816.OFFSET_CDATA, (short) 0);
					companyPubKey.setW(buf, offset[0], offset[1]);
					
					companySignature.init(companyPubKey, Signature.MODE_VERIFY);
					break;
				case CMD_RANDSEED:  // set the random seed
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
			// Reception commands
			if (buf[ISO7816.OFFSET_CLA] == CLA_RECEPTION) {
				// First we need an initialization command
				if (buf[ISO7816.OFFSET_INS] != CMD_REC_INIT && !receptionInitialized[0]) {
					ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
				}
				
				switch (buf[ISO7816.OFFSET_INS]) {
				case CMD_REC_INIT:  // initialize communication
					// save the nonce in tmp1
					Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, tmp1, (short)0, (short) NONCE_LENGTH);
					// Copy cardID and new nonce to the response buffer
					Util.setShort(buf, (short) 0, cardID);
					// Generate and store new card nonce
					random.generateData(buf, (short) 2, (short) NONCE_LENGTH);
					Util.arrayCopy(buf, (short) 2, nonce, (short) 0, (short) NONCE_LENGTH);
					// sign the terminal nonce and put it in the response buffer
					short sigLen = cardSignature.sign(
						tmp1, (short)0, (short) NONCE_LENGTH, buf, (short) (2+NONCE_LENGTH)
					);
					short totLen = (short) (sigLen + 2 + NONCE_LENGTH);

					short le = apdu.setOutgoing();
					if (le < totLen) {
						ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
					}
					apdu.setOutgoingLength(totLen);					 
					apdu.sendBytes((short)0, totLen);
					
					receptionInitialized[0] = true;
					break;
				case CMD_REC_GET_KM:  // get kilometer counting
					verifyCommand(buf);

					Util.arrayCopy(kilometers, (short) 0, tmp2, (short) 0, KM_LENGTH);
					
					sendResponse(buf, apdu, tmp2, (short) KM_LENGTH);
					break;
				case CMD_REC_RESET_KM:  // reset kilometer counting
					verifyCommand(buf);
					
					JCSystem.beginTransaction();
					for (short i=0; i<KM_LENGTH; i++) {
						kilometers[i] = 0;
					}
					JCSystem.commitTransaction();
					
					sendResponse(buf, apdu, null, (short) 0);
					break;
				case CMD_REC_CHECK_INUSE:  // check inUse flag
					verifyCommand(buf);
					
					tmp2[0] = (byte) (inUse ? 1 : 0);  // return inUse flag
					
					sendResponse(buf, apdu, tmp2, (short) 1);
					break;
				case CMD_REC_ADD_CERT:  // add certificate for vehicle, get public key and counter
					verifyCommand(buf, (short) 3);
										
					findOffset(buf, (short) (ISO7816.OFFSET_CDATA+NONCE_LENGTH), (short) 1);
					Util.arrayCopy(buf, offset[0], vehicleCert, (short) 0, offset[1]);
					vehicleCertLength = offset[1];
					
					findOffset(buf, (short) (ISO7816.OFFSET_CDATA+NONCE_LENGTH), (short) 2);
					vehiclePubKey.setW(buf, offset[0], offset[1]);
					vehicleSignature.init(vehiclePubKey, Signature.MODE_VERIFY);
					
					findOffset(buf, (short) (ISO7816.OFFSET_CDATA+NONCE_LENGTH), (short) 3);
					Util.arrayCopy(buf, offset[0], certCounter, (short) 0, offset[1]);
					
					isAssociated = true;  // only at the end set flag to true
					
					sendResponse(buf, apdu, null, (short)0);
					break;				
				case CMD_REC_DEL_CERT:  // delete certificate for vehicle, public key and counter
					verifyCommand(buf);
					
					isAssociated = false;  // first set flag to false
					Util.arrayFillNonAtomic(vehicleCert, (short) 0, (short) vehicleCert.length, (byte) 0);
					
					sendResponse(buf, apdu, null, (short)0);
					break;
				default:
					ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
				}
			}
			else if (buf[ISO7816.OFFSET_CLA] == CLA_VEHICLE) {
				short le;
				short len;
				short sigLen;
				boolean verified;
				
				// Return an error if the card is not associated to a vehicle
				if (!isAssociated) {
					ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
				}
				switch (buf[ISO7816.OFFSET_INS]) {
				case CMD_VEH_INIT:
					vehicleInitialized[0] = 1;
					
					// Put card public key in the buffer (length + actual data)
					len = cardPubKey.getW(buf, (short) 1);
					buf[0] = (byte) len;
					len++;
					
					// Put certCounter in the buffer
					buf[len] = COUNTER_LENGTH;
					len++;
					Util.arrayCopy(certCounter, (short) 0, buf, (short) len, COUNTER_LENGTH);
					len += COUNTER_LENGTH;
					
					// Put vehicleCert in the buffer
					buf[len] = (byte) vehicleCertLength;
					len++;
					Util.arrayCopy(vehicleCert, (short) 0, buf, (short) len, vehicleCertLength);
					len += vehicleCertLength;
					
					// send it!
					le = apdu.setOutgoing();
					if (le < len) {
						ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
					}
					apdu.setOutgoingLength(len);					 
					apdu.sendBytes((short)0, len);
					break;
				case CMD_VEH_AUTHCARD:
					// allow performing commands only in order
					if (vehicleInitialized[0] != 1)
						ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
					vehicleInitialized[0] = 2;
					
					// save the terminal nonce in tmp1
					Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, tmp1, (short) 0, (short) NONCE_LENGTH);
					// copy also the vehicle public key in tmp1
					short vehiclePubKeyLength = vehiclePubKey.getW(tmp1, NONCE_LENGTH);
					
					// Generate and store new card nonce
					random.generateData(buf, (short) 0, (short) NONCE_LENGTH);
					Util.arrayCopy(buf, (short) 0, nonce, (short) 0, (short) NONCE_LENGTH);
					
					// Sign terminal nonce || vehicle public key
					sigLen = cardSignature.sign(
						tmp1, (short)0, (short) (NONCE_LENGTH+vehiclePubKeyLength),
						buf, (short) (NONCE_LENGTH)
					);
					len = (short) (sigLen + NONCE_LENGTH);
					
					// send all the things
					le = apdu.setOutgoing();
					if (le < len) {
						ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
					}
					apdu.setOutgoingLength(len);					 
					apdu.sendBytes((short)0, len);
					break;
				case CMD_VEH_AUTHVEHICLE:
					// allow performing commands only in order
					if (vehicleInitialized[0] != 2)
						ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
					vehicleInitialized[0] = 3;
					
					// save the terminal nonce in tmp1
					Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, tmp1, (short) 0, (short) NONCE_LENGTH);
					
					// get signature
					findOffset(buf, (short) (ISO7816.OFFSET_CDATA+NONCE_LENGTH), (short) 0);
					verified = vehicleSignature.verify(
						nonce, (short) 0, NONCE_LENGTH, buf, offset[0], offset[1]
					);
					if (!verified) {
						ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
					}
					
					// Set inUse at first
					inUse = true;
					
					// Generate and store new card nonce
					random.generateData(buf, (short) 0, (short) NONCE_LENGTH);
					Util.arrayCopy(buf, (short) 0, nonce, (short) 0, (short) NONCE_LENGTH);
					
					// Sign terminal nonce
					sigLen = cardSignature.sign(tmp1, (short)0, (short) NONCE_LENGTH, buf, (short) NONCE_LENGTH);
					
					// send all the things
					len = (short) (sigLen + NONCE_LENGTH);
					le = apdu.setOutgoing();
					if (le < len) {
						ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
					}
					apdu.setOutgoingLength(len);					 
					apdu.sendBytes((short)0, len);
					break;
				case CMD_VEH_SAVEKM:
					// allow performing commands only in order
					if (vehicleInitialized[0] != 3)
						ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
					
					// save the terminal nonce in tmp1
					Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, tmp1, (short) 0, NONCE_LENGTH);
				
					// get driven kilometers
					findOffset(buf, (short) (ISO7816.OFFSET_CDATA+NONCE_LENGTH), (short) 0);
					byte[] kmToAdd = tmp3;
					Util.arrayCopy(buf, offset[0], kmToAdd, (short) 0, offset[1]);
					
					byte[] dataToVerify = tmp2;
					Util.arrayCopy(nonce, (short) 0, dataToVerify, (short) 0, NONCE_LENGTH);
					Util.arrayCopy(kmToAdd, (short) 0, dataToVerify, NONCE_LENGTH, KM_LENGTH);
					
					// get signature of nonce||kilometers
					findOffset(buf, (short) (ISO7816.OFFSET_CDATA+NONCE_LENGTH), (short) 1);
					verified = vehicleSignature.verify(
						dataToVerify, (short) 0, (short) (NONCE_LENGTH+KM_LENGTH),
						buf, offset[0], offset[1]
					);
					if (!verified) {
						ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
					}
					
					JCSystem.beginTransaction();
					if (!sumByteArrays(kilometers, kmToAdd, KM_LENGTH)) {  // there was an overflow!
						JCSystem.abortTransaction();
						ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
					}
					JCSystem.commitTransaction();
					
					inUse = false;
					
					// Copy the kilometer into the data to sign buffer
					Util.arrayCopy(kmToAdd, (short) 0, tmp1, NONCE_LENGTH, KM_LENGTH);
					
					// Sign terminal nonce + amount of km
					sigLen = cardSignature.sign(tmp1, (short)0, (short) (NONCE_LENGTH+KM_LENGTH), buf, (short) 0);
					
					// send all the things
					len = sigLen;
					le = apdu.setOutgoing();
					if (le < len) {
						ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
					}
					apdu.setOutgoingLength(len);					 
					apdu.sendBytes((short)0, len);
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
			length = (short) (buf[base] & 0xFF);
		}
		offset[0] = (short) (base + 1);
		offset[1] = length;
	}
	
	void initKey(ECKey key) {
		key.setA(CURVE_A, (short)0, (short) CURVE_A.length);
		key.setB(CURVE_B, (short)0, (short) CURVE_B.length);
		key.setR(CURVE_R, (short)0, (short) CURVE_R.length);
		key.setFieldF2M(CURVE_P[0],CURVE_P[1],CURVE_P[2]);
		key.setG(CURVE_G, (short)0, (short) CURVE_G.length);
	}
	
	void verifyCommand(byte[] buf) {
		verifyCommand(buf, (short) 0);
	}
	
	void verifyCommand(byte[] buf, short arguments) {		
		// save the terminal nonce in tmp1
		Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, tmp1, (short) 0, (short) NONCE_LENGTH);

		// use tmp2 as buffer for data to verify
		byte[] dataToVerify = tmp2;
		short lengthToVerify = (short) (NONCE_LENGTH + 1);

		// Copy old card nonce
		Util.arrayCopy(nonce, (short) 0, dataToVerify, (short) 0, (short) NONCE_LENGTH);		
		dataToVerify[NONCE_LENGTH] = buf[ISO7816.OFFSET_INS];  // Copy command

		// find payload
		findOffset(buf, (short) (ISO7816.OFFSET_CDATA+NONCE_LENGTH), (short) 1);
		if (offset[1] != 0) {  // if there is a payload in the command
			// get all the payload parameters in the verification buffer
			for (short i=0; i<arguments; i++) {
				findOffset(buf, (short) (ISO7816.OFFSET_CDATA+NONCE_LENGTH), (short) (1+i));
				Util.arrayCopy(
					buf, offset[0], dataToVerify, lengthToVerify, offset[1]
				);
				lengthToVerify += offset[1];
			}
		}

		findOffset(buf, (short) (ISO7816.OFFSET_CDATA+NONCE_LENGTH), (short) 0);
		boolean verified = companySignature.verify(
			dataToVerify, (short) 0, lengthToVerify, buf, offset[0], offset[1]
		);
		
		if (!verified)
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
	}
	
	void sendResponse(byte[] buf, APDU apdu, byte[] response, short responseLength) {
		// tmp1 contains the terminal nonce
		byte[] terminalNonce = tmp1;
		
		// create new nonce
		random.generateData(buf, (short) 0, (short)NONCE_LENGTH);
		
		// Store new card nonce into nonce
		Util.arrayCopy(buf, (short) 0, nonce, (short) 0, (short)NONCE_LENGTH);
		
		if (response != null) {
			// Set size of response payload
			Util.arrayCopy(response, (short) 0, terminalNonce, (short) NONCE_LENGTH, responseLength);
			Util.arrayCopy(response, (short) 0, buf, (short) (NONCE_LENGTH+1), responseLength);
		}
		buf[NONCE_LENGTH] = (byte) responseLength;
		
		// sign new nonce
		short sigLen = cardSignature.sign(
			terminalNonce,  // data to sign
			(short) 0,  // start of data to sign
			(short) (NONCE_LENGTH + responseLength),  // length to sign is nonce + response
			buf,  // destination
			(short) (NONCE_LENGTH + 1 + responseLength + 1)  // put the signature after nonce, response length, response data and signature length  
		);
		buf[NONCE_LENGTH + 1 + responseLength] = (byte) sigLen;  // write signature length
		
		// total length is nonce + response length + response data + signature length + signature data
		short totLen = (short) (NONCE_LENGTH + 1 + responseLength + 1 + sigLen);
		short le = apdu.setOutgoing();
		
		if (le < totLen) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}
		
		apdu.setOutgoingLength(totLen);					 
		apdu.sendBytes((short)0, totLen);
	}
	
	boolean sumByteArrays(byte[] first, byte[] second, short len) {
		if (second[0] < 0)  // do not allow negative numbers (first byte needs to be positive)
			return false;
		
		byte carry = 0;
		short currSum = 0;
		for (short i=(short) (len-1); i>=0; i--) {
			currSum = (short) ((first[i] & 0xFF) + (second[i] & 0xFF) + (carry & 0xFF));
			carry = (byte) (currSum >> 8);
			first[i] = (byte) currSum;
		}
		// check for overflows
		return (first[0] >= 0) && (carry == 0);
	}
}