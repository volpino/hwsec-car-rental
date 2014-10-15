package terminal.crypto;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;

public class EECKeyGenerator {

	KeyPairGenerator g;
	KeyPair pair;

	/*
	 * Define a generator which produce a "prime192v1", size: 192bit
	 */
	public EECKeyGenerator() {
		try {
			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
			ECGenParameterSpec ecGenSpec = new ECGenParameterSpec("prime192v1");
			g = KeyPairGenerator.getInstance("ECDSA", "BC");
			g.initialize(ecGenSpec, new SecureRandom());
			pair = g.generateKeyPair();

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
		}
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

	public void printPair() {
		System.out.println(pair.getPrivate());
		System.out.println(pair.getPublic());
	}

	public static void main(String[] args) {
		EECKeyGenerator gen = new EECKeyGenerator();
		gen.printPair();
	}

}
