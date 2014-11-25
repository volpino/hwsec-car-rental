package terminal.commands;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import terminal.crypto.ECCKeyGenerator;
import terminal.utils.Conversions;
import terminal.utils.Log;

/**
 * Class that handles the commands for the personalization phase of the card
 * 
 * @author Federico Scrinzi
 * @author Leon Schoorl
 *
 */
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
	
	/**
	 * Sets the card ID with a random value generated on the terminal side
	 * 
	 * @throws Exception
	 */
	private void setCardID() throws Exception {
		cardID = new byte[2];
		random.nextBytes(cardID);
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_CARDID, 0x00, 0x00, cardID, 2)
		);
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response. Is the card already issued?");
		}
		Log.info("Card ID set to " + Conversions.bytesToHex(cardID));
	}
	
	/**
	 * Generates an ECC keypair on the terminal and sends it to the card.
	 * Saves the public key on the terminal.
	 * 
	 * @throws Exception
	 */
	private void setCardKeyPair() throws Exception {
		ByteArrayOutputStream data = new ByteArrayOutputStream();  // APDU data
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

		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_CARDKEYS, 0x00, 0x00, data.toByteArray())
		);
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response");
		}
		Log.info("Keypair generated and sent to the smartcard");
	}
	
	/**
	 * Sends the company public key to the card
	 * 
	 * @throws Exception
	 */
	private void setCompanyPublicKey() throws Exception {
		ByteArrayOutputStream data = new ByteArrayOutputStream();  // APDU data
		KeyPair pair = ECCKeyGenerator.loadKeys("keys/master", "company");			
		ECPublicKey pub = (ECPublicKey) pair.getPublic();
		byte[] pubEncoded = Conversions.encodePubKey(pub);
		data.write(pubEncoded.length);
		data.write(pubEncoded);	
		
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_COMPANYPUB, 0x00, 0x00, data.toByteArray())
		);
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response");
		}
		Log.info("Company public key sent to the smartcard");
	}
	
	/**
	 * Sends an 8-byte random seed to the card
	 * 
	 * @throws Exception
	 */
	private void setRandomSeed() throws Exception {
		byte[] seed = new byte[8];
		random.nextBytes(seed);
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_ISSUE, CMD_RANDSEED, 0x00, 0x00, seed)
		);
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response");
		}
		Log.info("Random seed set");
	}
	
	/**
	 * Handles all the issuance process.
	 * - set the card ID
	 * - generate and set the card keypair
	 * - set the company public key
	 * - set the random seed 
	 * 
	 * @throws Exception
	 */
	public void doIssuance() throws Exception {
		setCardID();
		setCardKeyPair();
		setCompanyPublicKey();
		setRandomSeed();
	}
}
