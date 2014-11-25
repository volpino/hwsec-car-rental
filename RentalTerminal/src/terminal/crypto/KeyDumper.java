package terminal.crypto;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldF2m;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;
import java.util.Arrays;

/**
 * Class for getting information about an ECC key.
 * Used for debugging purposes
 * 
 * @author Leon Schoorl
 *
 */
public class KeyDumper {
	public static void main(String[] args) {
		System.out.println("Loading master key pair..");
		try {
			KeyPair keys = ECCKeyGenerator.loadKeys("data/master", "company");
			System.out.println("DONE");
			ECPublicKey pub = (ECPublicKey) keys.getPublic();
			ECParameterSpec params = pub.getParams();
			EllipticCurve curve = params.getCurve();
			System.out.println("public key: " + keys.getPublic());
			System.out.println("key format: " + keys.getPublic().getFormat());
			//System.out.println("encoded: " + Arrays.toString(keys.getPublic().getEncoded()));
			show("W.X", pub.getW().getAffineX());
			show("W.Y", pub.getW().getAffineY());
			show("A", curve.getA());
			show("B", curve.getB());
			show("G.X", params.getGenerator().getAffineX());
			show("G.Y", params.getGenerator().getAffineY());
			show("R", params.getOrder());
			show("(fieldF)P",  ((ECFieldF2m)curve.getField()) .getMidTermsOfReductionPolynomial() );

			ECPrivateKey priv = (ECPrivateKey) keys.getPrivate();
			params = priv.getParams();
			curve = params.getCurve();
			System.out.println("private key: " + priv);
			System.out.println("key format: " + priv.getFormat());
			//System.out.println("encoded: " + Arrays.toString(priv.getEncoded()));
			show("S", priv.getS());
			show("A", curve.getA());
			show("B", curve.getB());
			show("G.X", params.getGenerator().getAffineX());
			show("G.Y", params.getGenerator().getAffineY());
			show("R", params.getOrder());
			show("(fieldF)P",  ((ECFieldF2m)curve.getField()) .getMidTermsOfReductionPolynomial() );
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void show(String str, BigInteger n) {
		System.out.println(str + "("+ n.toByteArray().length+"):\t" + Arrays.toString(n.toByteArray()));
	}
	
	public static void show(String str, int[] n) {
		System.out.println(str + "("+ n.length+"):\t" + Arrays.toString(n));
	}
}
