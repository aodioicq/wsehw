package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;


public class Spearman {
	static Map<Integer, Double> pagerank_sp = new HashMap<Integer, Double>();
	static Map<Integer, Integer> numberofviewd_sp = new HashMap<Integer, Integer>();
	private static int maxDocs;

	public static void loadPR(String pageRank) throws NumberFormatException, IOException {
		//String pageRank = "data/pagerank.txt";
		String line;
		String[] parsed;
		maxDocs = 0;
		File pr = new File(pageRank);
		BufferedReader br = new BufferedReader(new FileReader(
				pr.getAbsoluteFile()));
		while ((line = br.readLine()) != null) {
			maxDocs++;
			parsed = line.split("\t");
			pagerank_sp.put(Integer.parseInt(parsed[0]), Double.parseDouble(parsed[1]));
		}
		br.close();
	}

	public static void loadNV(String numViews) throws NumberFormatException, IOException {
	//	String numViews = "data/numview.txt";
		String line;
		String[] parsed;
		File nv = new File(numViews);
		BufferedReader br = new BufferedReader(new FileReader(
				nv.getAbsoluteFile()));
		while ((line = br.readLine()) != null) {
			parsed = line.split("\t");
			numberofviewd_sp.put(Integer.parseInt(parsed[0]), Integer.parseInt(parsed[1]));
		}
		br.close();
	}

	public static void page(int numberofdocs) {// the total number of docs in
		
		int n = numberofdocs;
		int i, j;

		int x[] = new int[n];
		int y[] = new int[n];


		Map<Integer, Integer> hm3 = new HashMap<Integer, Integer>();
		Map<Integer, Integer> hm4 = new HashMap<Integer, Integer>();
		

		// ////// spearsman starts:
		pagerank_sp = sortByValues(pagerank_sp);

		i = 0;
		while (i < n) {

			for (Entry<Integer, Double> entry : pagerank_sp.entrySet()) {

				i++;
				hm3.put(i, entry.getKey());

			}
		}

		hm3 = sortByValues(hm3);
		j = n - 1;
		while (j > 0) {

			for (Entry<Integer, Integer> entry : hm3.entrySet()) {

				x[j] = entry.getKey();
				j--;
			}

		}

		// number of viewed starts

		numberofviewd_sp = sortByValues(numberofviewd_sp);

		// System.out.println("The docs and their rankings:");
		i = 0;
		while (i < n) {
			for (Entry<Integer, Integer> entry : numberofviewd_sp.entrySet()) {
				// System.out.println("Doc:"+entry.getKey()+ ", Rank:"+(i+1));
				i++;
				hm4.put(i, entry.getKey());
			}
		}

		hm4 = sortByValues(hm4);
		// System.out.println("my order:");
		j = n - 1;
		while (j > 0) {

			for (Entry<Integer, Integer> entry : hm4.entrySet()) {
				// System.out.println("Rank:"+entry.getKey()
				// +", Doc: "+entry.getValue());
				y[j] = entry.getKey();
				j--;
			}

		}
		// / number of viewed ends.

		// spersman starts.
		int sum_spearman = 0;
		for (int k = 0; k < n; k++) {
			sum_spearman = sum_spearman + (x[k] - y[k]) * ((x[k] - y[k]));
		}
		double ro = 1 - 6 * sum_spearman / n * (n * n - 1);
		System.out.println("the Spearman rank correlation:" + ro);
	}

	public static <K, V extends Comparable<V>> Map<K, V> sortByValues(
			final Map<K, V> map) {
		Comparator<K> valueComparator = new Comparator<K>() {
			public int compare(K k1, K k2) {
				int compare = map.get(k2).compareTo(map.get(k1));
				if (compare == 0)
					return 1;
				else
					return compare;
			}
		};
		Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
		sortedByValues.putAll(map);
		return sortedByValues;
	}

	public static void main(String[] args) throws NumberFormatException, IOException {

		loadPR(args[0]);

		loadNV(args[1]);

		page(maxDocs);


	}
}
