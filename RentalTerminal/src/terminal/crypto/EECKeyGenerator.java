package terminal.crypto;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

//Temporary these libraries are not used
//import org.bouncycastle.jce.ECNamedCurveTable;
//import org.bouncycastle.jce.spec.ECParameterSpec;

public class EECKeyGenerator {

	KeyPairGenerator g;
	KeyPair pair;

	/*
	 * Define a generator which produce a "prime192v1", size: 192bit
	 */
	public EECKeyGenerator() throws Exception {
		
			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
			ECGenParameterSpec ecGenSpec = new ECGenParameterSpec("prime192v1");
			g = KeyPairGenerator.getInstance("ECDSA", "BC");
			g.initialize(ecGenSpec, new SecureRandom());
			pair = g.generateKeyPair();
	}

	public KeyPair getKeyPair() {
		return pair;
	}
	
	public PublicKey getPublicKey() {
		return pair.getPublic();
	}
	
	public PrivateKey getPrivateKey() {
		return pair.getPrivate();
	}
	
	// Static method to be used in the graphic interface
	public static KeyPair generateKeys() throws Exception{
		EECKeyGenerator gen = new EECKeyGenerator();
		KeyPair sc = gen.getKeyPair();
		return sc;
	}

	// Testing purpose
	public void printPair() {
		System.out.println(pair.getPrivate());
		System.out.println(pair.getPublic());
	}
	public static void main(String[] args) {
		try {
			EECKeyGenerator gen = new EECKeyGenerator();
			gen.printPair();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
