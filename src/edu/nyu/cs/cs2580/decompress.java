package edu.nyu.cs.cs2580;

public class decompress {

	public Integer de_convert(String s) {
		int p = 0;
		int result = 0;

		int len = s.length();
		if (len <= 2) {
			p = Integer.parseInt(s, 16);
			int k1 = p - 128;
			result = k1;
		} else if (len > 2 && len <= 5) {

			String s1 = s.substring(2, len);
			p = Integer.parseInt(s1, 16);
			result = p;
		} else if (len > 5 && len <= 6) {
 
			String s1 = s.substring(2, len);
			p = Integer.parseInt(s1, 16);
			result = p;
		}

		return result;

	}
}