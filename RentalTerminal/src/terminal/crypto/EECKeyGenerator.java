package terminal.crypto;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;

public class EECKeyGenerator {

	KeyPairGenerator g;
	KeyPair pair;

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

	/**
	 * Testing purpose
	 * 
	 * @return Returns EEC keyPair
	 */
	public KeyPair getKeyPair() {
		return pair;
	}

	public void print() {
		System.out.println(pair.toString());
	}

	public static void main(String[] args) {
		EECKeyGenerator gen = new EECKeyGenerator();
		System.out.println(gen.toString());
	}

}
