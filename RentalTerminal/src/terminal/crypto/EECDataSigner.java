package terminal.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;

public class EECDataSigner {
	
	public byte[] signData(byte[] data, PrivateKey key){
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		// Final signature as byte array
		byte[] realSig = null;
		
		try {
			
			Signature sig = Signature.getInstance("SHA256withECDSA", "BC");
			
			sig.initSign(key);
			
			sig.update(data);
			
			realSig = sig.sign();
		
		} catch (NoSuchAlgorithmException e) {
			 System.err.println("Caught exception " + e.toString());
		} catch (NoSuchProviderException e) {
			 System.err.println("Caught exception " + e.toString());
		} catch (InvalidKeyException e) {
			 System.err.println("Caught exception " + e.toString());
		} catch (SignatureException e) {
			 System.err.println("Caught exception " + e.toString());
		}
		
		return realSig;
	}
}
