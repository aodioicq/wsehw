package edu.nyu.cs.cs2580;

public class compress {

	public String convert(int i) {
		String hex = "";

		// System.out.println("integer: " +i);
		String s = Integer.toBinaryString(i);
		String sNew = "";
		StringBuffer strbuffer2 = new StringBuffer();
		StringBuffer strbuffer3 = new StringBuffer();
		StringBuffer strbuffer;
		strbuffer = new StringBuffer(s);
		int len1 = strbuffer.length();
		int numShifts = 0;

		int initLoc = 0;

		if (len1 <= 7) {
			strbuffer2.append("1");
			for (int k = 0; k < 7 - len1; k++) {
				strbuffer2.append("0");
			}
			while (initLoc < len1) {
				strbuffer2.append(strbuffer.charAt(initLoc));
				initLoc++;
			}

			strbuffer = strbuffer2;

		} else {
			for (int j = 0; j < len1; j++) {
				if (j % 6 == 0 && j != 0) {
					if (numShifts == 0) {
						strbuffer2.append("1");
						numShifts++;
					} else {
						strbuffer2.append("0");
					}
				} else {
					strbuffer2.append(strbuffer.charAt(j));
				}
			}

		}

		s = strbuffer.toString();
		int binarynum = Integer.parseInt(s, 2);
		String hexk1 = "";
		String hexk2 = "01";
		String hexk3 = "01";
		String hexk4 = "01";
		if (i < Math.pow(2, 7)) {
			hex = Integer.toHexString(binarynum);
		} else if (i >= Math.pow(2, 7) && i < Math.pow(2, 14)) {
			hex = Integer.toHexString(binarynum);
			hex = hexk2.concat(hex);
		} else if (i >= Math.pow(2, 7) && i < Math.pow(2, 14)) {
			hex = Integer.toHexString(binarynum);
			hex = hexk2.concat(hex);
		} else if (i >= Math.pow(2, 14) && i < Math.pow(2, 21)) {
			hex = Integer.toHexString(binarynum);
			hex = hexk3.concat(hex);
		} else if (i >= Math.pow(2, 21) && i < Math.pow(2, 28)) {
			hex = Integer.toHexString(binarynum);
			hex = hexk4.concat(hex);
		}
		return hex;
		// System.out.println("hex: "+hex);
	}
}