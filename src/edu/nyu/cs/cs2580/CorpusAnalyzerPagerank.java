package edu.nyu.cs.cs2580;

import java.io.IOException;

import edu.nyu.cs.cs2580.SearchEngine.Options;


 private  String corpusFile="data/wiki";
   private  HashMap<String,Integer> postingMap = new HashMap<String,Integer>();
	 private  HashMap<Integer,Integer> numViewMap = new HashMap<Integer,Integer>();
	 private  String newline = System.getProperty("line.separator");
	 private  int maxDocs;
	 private  int [][] matrix;
/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {
  public CorpusAnalyzerPagerank(Options options) {
    super(options);
  }

  /**
   * This function processes the corpus as specified inside {@link _options}
   * and extracts the "internal" graph structure from the pages inside the
   * corpus. Internal means we only store links between two pages that are both
   * inside the corpus.
   * 
   * Note that you will not be implementing a real crawler. Instead, the corpus
   * you are processing can be simply read from the disk. All you need to do is
   * reading the files one by one, parsing them, extracting the links for them,
   * and computing the graph composed of all and only links that connect two
   * pages that are both in the corpus.
   * 
   * Note that you will need to design the data structure for storing the
   * resulting graph, which will be used by the {@link compute} function. Since
   * the graph may be large, it may be necessary to store partial graphs to
   * disk before producing the final graph.
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

		int count = 0;
		while ((line = br.readLine()) != null) {
			parseLine = line.split(" ");

			if (parseLine[1].contains("FILE:")) {
				parseLine[1] = parseLine[1].replaceAll("FILE:", "");
			}
			if (parseLine[1].contains("File:")) {
				parseLine[1] = parseLine[1].replaceAll("File:", "");
			}
			if (parseLine[1].contains("wikipedia:")) {
				parseLine[1] = parseLine[1].replaceAll("wikipedia:",
						"Wikipedia_");
			}
			if (parseLine[1].contains("zh:")) {
				parseLine[1] = parseLine[1].replaceAll("zh:", "");
			}
			if (!parseLine[1].startsWith(".") && !parseLine[1].startsWith("/")
					&& checkUrl(parseLine[1])) {
				parseLine[1] = URLDecoder.decode(parseLine[1], "UTF-8");
				parseLine[1] = StringEscapeUtils.unescapeHtml4(parseLine[1]);
				if (parseLine[1].contains("\\")) {
					parseLine[1] = parseHex(parseLine[1]);
				}
			}

			if (postingMap.containsKey(parseLine)) {
				numViewMap.put(postingMap.get(parseLine),
						Integer.parseInt(parseLine[2]));
			}

		}

	}

	private String parseHex(String line) {

		String[] splitHex = line.split("\\\\");
		String parsed = "";

		for (String s : splitHex) {
			if (s.startsWith("x")) {
				parsed += hex(s.substring(1, 3));
				if (splitHex[0].length() > 2) {
					parsed += s.substring(3);

				}
			} else {
				parsed += s;
			}
		}
		return parsed;
	}

	// converts the hex string to decimal and then to string
	private String hex(String line) {

		StringBuilder sb = new StringBuilder();
		StringBuilder temp = new StringBuilder();

		for (int i = 0; i < line.length() - 1; i += 2) {

			String output = line.substring(i, (i + 2));
			int decimal = Integer.parseInt(output, 16);
			sb.append((char) decimal);

			temp.append(decimal);
		}
		return sb.toString();
	}

	private boolean checkUrl(String line) {

		String[] parseLine = line.split("%");
		if (line.endsWith("%")) {
			return false;
		}
		for (int i = 1; i < parseLine.length; i++) {
			if (parseLine.length < 2) {
				return false;
			} else if (parseLine[i].length() < 2) {
				return false;
			} else if (!Character.isLetterOrDigit(parseLine[i].charAt(0))
					|| !Character.isLetterOrDigit(parseLine[i].charAt(1))) {
				return false;
			}
		}
		return true;
	}
  /**
   * This function computes the PageRank based on the internal graph generated
   * by the {@link prepare} function, and stores the PageRank to be used for
   * ranking.
   * 
   * Note that you will have to store the computed PageRank with each document
   * the same way you do the indexing for HW2. I.e., the PageRank information
   * becomes part of the index and can be used for ranking in serve mode. Thus,
   * you should store the whatever is needed inside the same directory as
   * specified by _indexPrefix inside {@link _options}.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException {
    System.out.println("Computing using " + this.getClass().getName());
    return;
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
    return null;
  }
}
