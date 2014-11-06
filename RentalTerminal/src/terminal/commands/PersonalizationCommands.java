package terminal.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import terminal.crypto.EECKeyGenerator;
import terminal.crypto.EECSignature;
import terminal.utils.Conversions;
import terminal.utils.Log;


public class PersonalizationCommands {
	public static final byte CLA_ISSUE = (byte) 0xB0;
	public static final byte CMD_CARDID = (byte) 0x00;
	public static final byte CMD_CARDKEYS = (byte) 0x01;
	public static final byte CMD_COMPANYPUB = (byte) 0x02;
	public static final byte CMD_RANDSEED = (byte) 0x03;

	CardCommunication comm;
	SecureRandom random;
	
	public PersonalizationCommands(CardCommunication c) {
		comm = c;
		random = new SecureRandom();
	}
	
	public void setCardID() {
		byte[] cardID = new byte[2];
		random.nextBytes(cardID);
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_CARDID, 0x00, 0x00, cardID)
		);
		Log.info("Card ID set to " + Arrays.toString(cardID));
	}
	
	public void setCardKeyPair() {
		ByteArrayOutputStream data = new ByteArrayOutputStream();  // APDU data
		try {
			KeyPair pair = EECKeyGenerator.generateKeys();	
			ECPublicKey pub = (ECPublicKey) pair.getPublic();
			ECPrivateKey priv = (ECPrivateKey) pair.getPrivate();

			byte[] pubEncoded = Conversions.encodePubKey(pub);
			data.write(pubEncoded.length);
			data.write(pubEncoded);
			
			// private key
			byte[] s = Conversions.padToFieldSize(priv.getS().toByteArray());
			data.write(s.length);
			data.write(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_CARDKEYS, 0x00, 0x00, data.toByteArray())
		);
		Log.info("Keypair generated and sent to the smartcard");
	}
	
	public void setCompanyPublicKey() {
		ByteArrayOutputStream data = new ByteArrayOutputStream();  // APDU data
		try {
			KeyPair pair = EECKeyGenerator.loadKeys("keys/master", "company");			
			ECPublicKey pub = (ECPublicKey) pair.getPublic();
			byte[] pubEncoded = Conversions.encodePubKey(pub);
			data.write(pubEncoded.length);
			data.write(pubEncoded);	
		} catch (Exception e) {
			e.printStackTrace();
		}
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_COMPANYPUB, 0x00, 0x00, data.toByteArray())
		);
		Log.info("Company public key sent to the smartcard");
	}
	
	public void setRandomSeed() {
		byte[] seed = new byte[8];
		random.nextBytes(seed);
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_RANDSEED, 0x00, 0x00, seed)
		);
		Log.info("Random seed set");
	}
	
	public void doIssuance() {
		setCardID();
		setCardKeyPair();
		setCompanyPublicKey();
		setRandomSeed();
	}
}
