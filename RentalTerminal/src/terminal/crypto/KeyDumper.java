package terminal.crypto;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;
import java.util.Arrays;

public class KeyDumper {

	public static void main(String[] args) {
		System.out.println("Loading master key pair..");
		try {
			EECKeyGenerator gen =  new EECKeyGenerator();
			KeyPair keys = EECKeyGenerator.loadKeys("ECDSA", "keys/cars","car1");
//			KeyPair keys = EECKeyGenerator.loadKeys("ECDSA", "keys/master","company");
			System.out.println("DONE");
			ECPublicKey pub = (ECPublicKey) keys.getPublic();
			ECParameterSpec params = pub.getParams();
			EllipticCurve curve = params.getCurve();
			System.out.println("public key: " + keys.getPublic());
			System.out.println("key format: " + keys.getPublic().getFormat());
			System.out.println("encoded: " + Arrays.toString(keys.getPublic().getEncoded()));
			show("W.X", pub.getW().getAffineX());
			show("W.Y", pub.getW().getAffineY());
			show("A", curve.getA());
			show("B", curve.getB());
			show("G.X", params.getGenerator().getAffineX());
			show("G.Y", params.getGenerator().getAffineY());
			show("R", params.getOrder());
			show("(fieldF)P",  ((ECFieldFp)curve.getField()) .getP() );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void show(String str, BigInteger n) {
		System.out.println(str + "("+ n.toByteArray().length+"):\t" + Arrays.toString(n.toByteArray()));
	}
}
