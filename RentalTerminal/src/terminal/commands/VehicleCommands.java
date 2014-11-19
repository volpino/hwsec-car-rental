package terminal.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import terminal.crypto.ECCKeyGenerator;
import terminal.crypto.ECCSignature;
import terminal.utils.CertCounter;
import terminal.utils.Conversions;

import terminal.utils.LogToFile;


public class VehicleCommands {
	public static final byte CLA_VEHICLE = (byte) 0xB2;
	public static final byte CMD_VEH_INIT = (byte) 0x00;
	public static final byte CMD_VEH_AUTHCARD = (byte) 0x01;
	public static final byte CMD_VEH_AUTHVEHICLE = (byte) 0x02;
	public static final byte CMD_VEH_SAVEKM = (byte) 0x03;

	
	public static final short NONCE_LENGTH = 8;
	
	CardCommunication comm;
	SecureRandom random;
	
	byte[] cardNonce;

	ECPublicKey cardKey;
	ECPublicKey companyKey;
	KeyPair vehicleKeypair;
	
	String carID;
	
	public VehicleCommands(CardCommunication c, String id) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		comm = c;
		random = new SecureRandom();
		carID = id;
		vehicleKeypair = ECCKeyGenerator.loadKeys("keys/cars", carID);
		companyKey = (ECPublicKey) ECCKeyGenerator.loadPublicKey("keys/master", "company");
	}
	
	void sendInit() throws Exception {
		byte[] nonce = new byte[NONCE_LENGTH];
		random.nextBytes(nonce);
		
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_VEHICLE, CMD_VEH_INIT, 0x00, 0x00, nonce, 255)
		);
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response");
		}
		byte[] buffer = response.getData();
	
		//Receive nonceB
		cardNonce = Arrays.copyOfRange(buffer, 0, NONCE_LENGTH);

		//Public card key
		cardKey = (ECPublicKey) ECCKeyGenerator.decodeKey(Conversions.getChunk(buffer, NONCE_LENGTH, 0));
		
		//Get Cert counter
		long certCounter = Conversions.bytesToLong(Conversions.getChunk(buffer, NONCE_LENGTH, 1));
		
		//Get Sc
		byte[] vehicleCert = Conversions.getChunk(buffer, NONCE_LENGTH, 2);
		
		//Get validation Certificate
		byte[] validationCert = Conversions.getChunk(buffer, NONCE_LENGTH, 3);
		
		//Check that new counter is higher than old one
		long currentCounter = CertCounter.getCarCounter(carID);
		if (certCounter >= currentCounter) {
			CertCounter.setCarCounter(carID, certCounter);
		}
		else {
			throw new SecurityException("CertCounter is lower than vehicle's one. Rejecting card");
		}
		
		ByteArrayOutputStream dataToVerify = new ByteArrayOutputStream();
		
		//Verify Signature Sc
		dataToVerify.write(Conversions.encodePubKey((ECPublicKey) vehicleKeypair.getPublic()));
		dataToVerify.write(Conversions.encodePubKey(cardKey));
		dataToVerify.write(Conversions.longToBytes(certCounter));
		
		boolean verified = ECCSignature.verifySig(dataToVerify.toByteArray(), companyKey, vehicleCert);
		if (!verified) {
			throw new SecurityException("Vehicle certificate is not valid. Rejecting card");
		}
		
		dataToVerify.reset();
		
		//Verify Signature of NonceA and Sc
		dataToVerify.write(nonce);
		dataToVerify.write(vehicleCert);
		
		verified = ECCSignature.verifySig(dataToVerify.toByteArray(), cardKey, validationCert);
		if (!verified) {
			throw new SecurityException("Signature of Nonce and Signature Sc is not valid!");
		}
		
		
		
		
	}
	
	void authenticateCard() throws Exception {
		byte[] nonce = new byte[NONCE_LENGTH];
		random.nextBytes(nonce);
		
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_VEHICLE, CMD_VEH_AUTHCARD, 0x00, 0x00, nonce, 255)
		);
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response");
		}
		
		byte[] buffer = response.getData();
		cardNonce = Arrays.copyOfRange(buffer, 0, NONCE_LENGTH);
		byte[] signature = Arrays.copyOfRange(buffer, NONCE_LENGTH, buffer.length);
		ByteArrayOutputStream dataToVerify = new ByteArrayOutputStream();
		dataToVerify.write(nonce);
		dataToVerify.write(Conversions.encodePubKey((ECPublicKey) vehicleKeypair.getPublic()));
		boolean verified = ECCSignature.verifySig(dataToVerify.toByteArray(), cardKey, signature);
		if (!verified) {
			throw new SecurityException("Card signature is not valid. Rejecting card");
		}
	}
	
	void authenticateVehicle() throws Exception {
		byte[] nonce = new byte[NONCE_LENGTH];
		random.nextBytes(nonce);
		byte[] signature = ECCSignature.signData(cardNonce, vehicleKeypair.getPrivate());
		
		ByteArrayOutputStream dataToSend = new ByteArrayOutputStream();
		dataToSend.write(nonce);
		dataToSend.write(signature.length);
		dataToSend.write(signature);
		
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_VEHICLE, CMD_VEH_AUTHVEHICLE, 0x00, 0x00, dataToSend.toByteArray(), 255)
		);
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response");
		}
		
		byte[] buffer = response.getData();
		cardNonce = Arrays.copyOfRange(buffer, 0, NONCE_LENGTH);
		signature = Arrays.copyOfRange(buffer, NONCE_LENGTH, buffer.length);
		boolean verified = ECCSignature.verifySig(nonce, cardKey, signature);
		if (!verified) {
			throw new SecurityException("Card signature is not valid. Rejecting card");
		}
	}
	
	void writeKilometers(long kilometers) throws Exception {
		byte[] nonce = new byte[NONCE_LENGTH];
		random.nextBytes(nonce);
		
		byte[] kmBytes = Conversions.longToBytes(kilometers);
		
		ByteArrayOutputStream dataToSign = new ByteArrayOutputStream();
		dataToSign.write(cardNonce);
		dataToSign.write(kmBytes);
		byte[] signature = ECCSignature.signData(dataToSign.toByteArray(), vehicleKeypair.getPrivate());
		
		ByteArrayOutputStream dataToSend = new ByteArrayOutputStream();
		dataToSend.write(nonce);
		dataToSend.write(kmBytes.length);
		dataToSend.write(kmBytes);
		dataToSend.write(signature.length);
		dataToSend.write(signature);
		
		ResponseAPDU response = comm.sendCommandAPDU(
			new CommandAPDU(CLA_VEHICLE, CMD_VEH_SAVEKM, 0x00, 0x00, dataToSend.toByteArray(), 255)
		);
		if (response.getSW() != 0x9000) {
			throw new Exception("Got invalid response");
		}
		
		ByteArrayOutputStream dataToVerify = new ByteArrayOutputStream();
		dataToVerify.write(nonce);
		dataToVerify.write(kmBytes);
		
		byte[] buffer = response.getData();
		boolean verified = ECCSignature.verifySig(dataToVerify.toByteArray(), cardKey, buffer);
		if (!verified) {
			LogToFile.write("Card signature is not valid. Writing of kilometers was not successful");
			throw new SecurityException("Card signature is not valid. Writing of kilometers was not successful");
			
		}
		
		
		LogToFile.write("Nonce: " + Arrays.toString(nonce));
		LogToFile.write("Kilometers: " + Arrays.toString(kmBytes));
		LogToFile.write("Signature: " + Arrays.toString(buffer));
		
	}
	
	public void startVehicle() throws Exception {
		sendInit();
		authenticateCard();
		authenticateVehicle();
	}
	
	public void stopVehicle(long kilometers) throws Exception {
		sendInit();
		authenticateCard();
		authenticateVehicle();
		writeKilometers(kilometers);
	}
}
