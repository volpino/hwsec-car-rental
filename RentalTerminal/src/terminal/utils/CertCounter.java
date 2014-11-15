package terminal.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class CertCounter {
	public static int getNewCounter() throws IOException {
		String path = "keys/master/company.counter";
		int result;
		try {
			Scanner in = new Scanner(new FileReader(path));
			result = in.nextInt();
			in.close();
		} catch (FileNotFoundException e) {
			result = 0;
		}
		FileWriter wr = new FileWriter(path);
		wr.write(String.valueOf(result + 1));
		wr.close();
		return result;
	}
	
	public static int getCarCounter(String carID) {
		Scanner in;
		try {
			in = new Scanner(new FileReader("keys/cars/" + carID + ".counter"));
		} catch (FileNotFoundException e) {
			return 0;
		}
		int ret = in.nextInt();
		in.close();
		return ret;
	}
	
	public static void setCarCounter(String carID, int counter) throws IOException {
		FileWriter wr = new FileWriter("keys/cars/" + carID + ".counter");
		wr.write(String.valueOf(counter));
		wr.close();
	}
}
