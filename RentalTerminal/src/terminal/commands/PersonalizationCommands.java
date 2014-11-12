package terminal.commands;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import javax.smartcardio.CommandAPDU;

import terminal.crypto.ECCKeyGenerator;
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
	byte[] cardID;
	
	public PersonalizationCommands(CardCommunication c) {
		comm = c;
		random = new SecureRandom();
	}
	
	void setCardID() {
		cardID = new byte[2];
		random.nextBytes(cardID);
		comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_CARDID, 0x00, 0x00, cardID, 2)
		);
		Log.info("Card ID set to " + Conversions.bytesToHex(cardID));
	}
	
	void setCardKeyPair() {
		ByteArrayOutputStream data = new ByteArrayOutputStream();  // APDU data
		try {
			KeyPair pair = ECCKeyGenerator.generateKeys();
			ECCKeyGenerator.savePublicKey(pair, "keys/customers", Conversions.bytesToHex(cardID));
			
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
		comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_CARDKEYS, 0x00, 0x00, data.toByteArray())
		);
		Log.info("Keypair generated and sent to the smartcard");
	}
	
	void setCompanyPublicKey() {
		ByteArrayOutputStream data = new ByteArrayOutputStream();  // APDU data
		try {
			KeyPair pair = ECCKeyGenerator.loadKeys("keys/master", "company");			
			ECPublicKey pub = (ECPublicKey) pair.getPublic();
			byte[] pubEncoded = Conversions.encodePubKey(pub);
			data.write(pubEncoded.length);
			data.write(pubEncoded);	
		} catch (Exception e) {
			e.printStackTrace();
		}
		comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_COMPANYPUB, 0x00, 0x00, data.toByteArray())
		);
		Log.info("Company public key sent to the smartcard");
	}
	
	void setRandomSeed() {
		byte[] seed = new byte[8];
		random.nextBytes(seed);
		comm.sendCommandAPDU(
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
