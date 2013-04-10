package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;


import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {
	public CorpusAnalyzerPagerank(Options options) {
		super(options);
	}

	private String corpusFile = "data/wiki";
	private HashMap<String, Integer> postingMap = new HashMap<String, Integer>();
	private HashMap<Integer, Integer> numViewMap = new HashMap<Integer, Integer>();
	private Map<Integer, Double> pagerank_sp = new HashMap<Integer, Double>();
	private String newline = System.getProperty("line.separator");
	private int maxDocs;
	private int[][] matrix;

	/**
	 * This function processes the corpus as specified inside {@link _options}
	 * and extracts the "internal" graph structure from the pages inside the
	 * corpus. Internal means we only store links between two pages that are
	 * both inside the corpus.
	 * 
	 * Note that you will not be implementing a real crawler. Instead, the
	 * corpus you are processing can be simply read from the disk. All you need
	 * to do is reading the files one by one, parsing them, extracting the links
	 * for them, and computing the graph composed of all and only links that
	 * connect two pages that are both in the corpus.
	 * 
	 * Note that you will need to design the data structure for storing the
	 * resulting graph, which will be used by the {@link compute} function.
	 * Since the graph may be large, it may be necessary to store partial graphs
	 * to disk before producing the final graph.
	 * 
	 * @throws IOException
	 */
	@Override
	public void prepare() throws IOException {
		System.out.println("Preparing " + this.getClass().getName());
		File root = new File(corpusFile);
		int docid = 0;

		for (File file : root.listFiles()) {
			postingMap.put(file.getName(), docid);
			docid++;
		}
		maxDocs = docid;

		crawl();
		createMatrix();

		return;
	}

	private void crawl() {

		File root = new File(corpusFile);
		String linkName;
		Vector<Integer> docLinks = new Vector<Integer>();
		for (File file : root.listFiles()) {
			if (isValidDocument(file)) {
				try {
					CorpusAnalyzer.HeuristicLinkExtractor link = new CorpusAnalyzer.HeuristicLinkExtractor(
							file);
					linkName = link.getLinkSource();

					while (linkName != null) {
						linkName = link.getNextInCorpusLinkTarget();
						if (postingMap.containsKey(linkName)) {
							int temp = postingMap.get(linkName);
							if (!docLinks.contains(temp)) {
								docLinks.add(temp);
							}

						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// if(!docLinks.isEmpty()) {
				writeToDisk(file.getName().charAt(0),
						postingMap.get(file.getName()), docLinks);
				docLinks.clear();
				// }
			}
		}
	}

	private void writeToDisk(char letter, Integer docId,
			Vector<Integer> docLinks) {
		// Test matrix
		docLinks.add(2);
		docLinks.add(4);

		File f = new File("data/index/");
		if (!f.exists()) {
			f.mkdir();
		}
		letter = Character.toUpperCase(letter);
		String ff = "data/index/" + letter + ".txt";
		File write = new File(ff);
		if (!write.exists()) {
			try {
				write.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// the true appends data
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(ff, true));
			bw.write(docId.toString());
			bw.write("\t");
			bw.write(docLinks.toString());
			bw.write(newline);
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void createMatrix() {
		matrix = new int[maxDocs][maxDocs];

		// read in all files
		File f = new File("data/index/");
		String line;
		String[] map;
		int n;
		String[] m;
		for (File file : f.listFiles()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(
						file.getAbsoluteFile()));
				while ((line = br.readLine()) != null) {
					map = line.split("\t");
					n = Integer.parseInt(map[0]);
					// remove the [] and ,
					m = map[1].substring(1, map[1].length() - 1).split(", ");
					for (String m1 : m) {
						matrix[n][Integer.parseInt(m1)] = 1;
					}
				}
				br.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private void print() {
		for (int i = 0; i < maxDocs; i++) {
			for (int j = 0; j < maxDocs; j++) {
				System.out.print(matrix[i][j] + "\t");
			}
			System.out.println("");
		}
	}

	private void numViews() throws IOException {
		String line;
		String[] parseLine;
		File log = new File("data/log/20130301-160000.log");
		BufferedReader br = new BufferedReader(new FileReader(
				log.getAbsoluteFile()));

		while ((line = br.readLine()) != null) {
			parseLine = line.split(" ");

			if (postingMap.containsKey(parseLine[1])) {
				numViewMap.put(postingMap.get(parseLine[1]),
						Integer.parseInt(parseLine[2]));
			}
		}
		br.close();
	}
	/**
	 * This function computes the PageRank based on the internal graph generated
	 * by the {@link prepare} function, and stores the PageRank to be used for
	 * ranking.
	 * 
	 * Note that you will have to store the computed PageRank with each document
	 * the same way you do the indexing for HW2. I.e., the PageRank information
	 * becomes part of the index and can be used for ranking in serve mode.
	 * Thus, you should store the whatever is needed inside the same directory
	 * as specified by _indexPrefix inside {@link _options}.
	 * 
	 * @throws IOException
	 */
	@Override
	public void compute() throws IOException {
		System.out.println("Computing using " + this.getClass().getName());

		double lamma = 0.1;// constant is given
		int n = maxDocs;
		int i, j;
		int temp[] = new int[n];
		int sum = 0;
		double[] r_vector = new double[n]; // initial rank vector;
		double g_matrix[][] = new double[n][n];// google matrix;
		double t_matrix[][] = new double[n][n]; // transition matrix based on
												// link matrix;
		double k_matrix[][] = new double[n][n]; // helper matrix;
		double page_rank[] = new double[n];// page rank vector;
		double iter_vec[] = new double[n];// helper
		double itera[][] = new double[n][n];// helper

		Map<Integer, Integer> hm3 = new HashMap<Integer, Integer>();
		Map<Integer, Integer> hm4 = new HashMap<Integer, Integer>();
		// Map<Integer,Integer> numberofviewd_sp = new
		// HashMap<Integer,Integer>();
		for (i = 0; i < n; i++) {
			for (j = 0; j < n; j++) {

				r_vector[j] = (double) 1 / n;
				k_matrix[i][j] = (double) 1 / n;

			}
		}

		int row_sum = 0;
		for (i = 0; i < n; i++) {
			for (j = 0; j < n; j++) {
				row_sum = row_sum + matrix[i][j];
			}
			temp[i] = row_sum;
			row_sum = 0;

		}

		// creating transition matrix;
		for (i = 0; i < n; i++) {
			for (j = 0; j < n; j++) {
				t_matrix[i][j] = (double) matrix[i][j] / temp[i];
				// System.out.println("["+i+"]["+j+"]= "+t_matrix[i][j]);
			}
		}

		// transpose the transition matrix;
		for (i = 0; i < n; i++) {
			for (j = i; j < n; j++) {
				double t = t_matrix[i][j];
				t_matrix[i][j] = t_matrix[j][i];
				t_matrix[j][i] = t;

			}
		}

		// calculating the google matrix;
		for (i = 0; i < n; i++) {
			for (j = 0; j < n; j++) {
				g_matrix[i][j] = (double) lamma * t_matrix[i][j]
						+ (double) (1 - lamma) * k_matrix[i][j];
				itera[i][j] = g_matrix[i][j];

			}

		}

		System.out.println("the page rank is with one iteration");
		double sum1 = 0.0;

		for (int c = 0; c < n; c++) {
			for (int d = 0; d < n; d++) {

				sum1 = sum1 + g_matrix[c][d] * r_vector[d];
				// System.out.println("sum is: "+sum1);
			}

			r_vector[c] = sum1;
			iter_vec[c] = r_vector[c];
			// System.out.println(r_vector[c]);
			// System.out.print("\n");
			pagerank_sp.put(c + 1, r_vector[c]);
			sum1 = 0.0;
		}

		/*
		 * System.out.println("page rank with two iterations:"); double
		 * sum2=0.0;
		 * 
		 * for (int c = 0 ; c < n ; c++ ) { for (int d = 0 ; d < n ; d++ ) {
		 * 
		 * sum2 = sum2 + itera[c][d]* iter_vec[d]; //
		 * System.out.println("sum is: "+sum1); }
		 * 
		 * 
		 * r_vector[c] = sum2; System.out.println(r_vector[c]);
		 * //System.out.print("\n"); sum2 = 0.0; }
		 */

	}

	/**
	 * During indexing mode, this function loads the PageRank values computed
	 * during mining mode to be used by the indexer.
	 * 
	 * @throws IOException
	 */
	@Override
	public Object load() throws IOException {
		System.out.println("Loading using " + this.getClass().getName());
		File pagerank = new File("data/pagerank.txt");
		File numview = new File("data/numview.txt");

		if (!pagerank.exists()) {
			pagerank.createNewFile();
		}
		if (!numview.exists()) {
			numview.createNewFile();
		}
		loadPr(pagerank);
		loadNV(numview);
		return null;
	}

	public void loadPr(File pagerank) throws IOException {
		String newline = System.getProperty("line.separator");
		String out = " ";
		TreeMap<Integer, Double> tm = new TreeMap<Integer, Double>(pagerank_sp);
		BufferedWriter bw = new BufferedWriter(new FileWriter(
				pagerank.getAbsoluteFile()));

		for (Entry<Integer, Double> entry : tm.entrySet()) {
			out = entry.getKey().toString() + "\t"
					+ entry.getValue().toString();
			bw.write(out);
			bw.write(newline);
		}
		bw.close();
	}

	public void loadNV(File numviews) throws IOException {
		String newline = System.getProperty("line.separator");
		String out = " ";
		TreeMap<Integer, Integer> tm = new TreeMap<Integer, Integer>(numViewMap);

		BufferedWriter bw = new BufferedWriter(new FileWriter(
				numviews.getAbsoluteFile()));

		for (Entry<Integer, Integer> entry : tm.entrySet()) {
			out = entry.getKey().toString() + "\t"
					+ entry.getValue().toString();
			bw.write(out);
			bw.write(newline);
		}
		bw.close();
	}
}
