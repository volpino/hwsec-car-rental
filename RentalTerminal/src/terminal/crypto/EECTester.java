package terminal.crypto;

import java.security.KeyPair;

public class EECTester {
	
	public static void main(String[] args){
		
		byte[] data = {(byte) 0x3B, (byte) 0x3B, (byte) 0x63, (byte) 0x61, (byte) 0x6C, (byte) 0x63, (byte) 0x01 };
		
		KeyPair keys = new EECKeyGenerator().getKeyPair();
		
		byte[] signedData;
		
		EECDataSigner signer = new EECDataSigner();
		EECSignatureVerifier verifier = new EECSignatureVerifier();
		
		signedData = signer.signData(data, keys.getPrivate());
		
		System.out.println("Signature: "+signedData);
		
		System.out.println("Verify Signature");
		boolean sigIsValid = verifier.verifySignature(signedData, keys.getPublic());
		System.out.println("Is valid: "+sigIsValid);
		
		
	}
}
