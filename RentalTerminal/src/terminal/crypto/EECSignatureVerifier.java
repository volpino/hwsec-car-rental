package terminal.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;

public class EECSignatureVerifier {
	
	public boolean verifySignature(byte[] sigToVerify, PublicKey key){
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		boolean validSignature = false;
		
		try {
			Signature sig = Signature.getInstance("SHA256withECDSA", "BC");
			sig.initVerify(key);
			sig.update(sigToVerify);
			
			validSignature = sig.verify(sigToVerify);
			
		} catch (InvalidKeyException e) {
			System.err.println("Caught exception " + e.toString());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return validSignature;
	}
}
