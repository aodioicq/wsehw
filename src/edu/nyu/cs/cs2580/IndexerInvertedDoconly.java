package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDoconly extends Indexer {

	private static HashMap<String, Vector<Integer>> _documents;
	private static HashMap<String, Integer> _terms;
	public Vector<DocumentIndexed> _allDocs;
	private String index_source;

	public IndexerInvertedDoconly(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}

	private String stem(String word) {
		if (word.endsWith("s")) {
			if (word.endsWith("sses")) {
				word = word.substring(0, word.length() - 2);
			} else if (word.endsWith("ies")) {
				word = word.substring(0, word.length() - 2);
			} else {
				word = word.substring(0, word.length() - 1);
			}
		}
		if (word.endsWith("eed")) {

		}
		if (word.endsWith("edly") || word.endsWith("ingly")) {
			word = word.substring(0, word.length() - 2);

		}
		if (word.endsWith("ed") || word.endsWith("ly")) {
			word = word.substring(0, word.length() - 2);
			if (word.endsWith("at")) {
				word = word + "e";
			} else if (word.endsWith("bl")) {
				word = word + "e";
			} else if (word.endsWith("iz")) {
				word = word + "e";
			}
		}
		return word;
	}

	@Override
	public void constructIndex() throws IOException {
		String corpusFile = _options._corpusPrefix;
		// String corpusFile="data/wiki";
		System.out.println("Construct index from: " + corpusFile);

		File root = new File(corpusFile);
		File[] files = root.listFiles();

		_documents = new HashMap<String, Vector<Integer>>();
		_allDocs = new Vector<DocumentIndexed>();
		// index_source = "data/simple/test.txt";
		String line = null;

		Vector<Integer> temp;
		int did = 0;
		int didIndex = 0;
		int part = 1;
		for (int i = 0; i < files.length; i++) {
			String filename = corpusFile + "/" + files[i].getName();
			System.out.println("reading " + filename);
			DocumentIndexed d = new DocumentIndexed(did);
			_allDocs.add(d);
			int pos = 0;
			String content = readToString(filename);
			content = Html2Text(content);
			Scanner s = new Scanner(content);
			while (s.hasNext()) {
				String word = s.next();
				word = stem(word.toLowerCase());
				temp = new Vector<Integer>();
				if (!_documents.containsKey(word)) {
					temp.add(did);
				} else {
					temp = _documents.get(word);

					temp.add(did);
				}
				_documents.put(word, temp);
			}

			if (did > part * 200) {
				saveToFile(part);
				part++;
				_documents.clear();
			}
			did++;
		}
		saveToFile(part);
	}

	public void saveToFile(int part) {
		String newline = System.getProperty("line.separator");
		char letter = 'a';
		String out = " ";
		TreeMap<String, Vector<Integer>> tm = new TreeMap<String, Vector<Integer>>(
				_documents);
		File f = new File("data/index/doc");
		if (!f.exists()) {
			f.mkdir();
		}
		try {

			BufferedWriter os = new BufferedWriter(new FileWriter("data/index/doc"
					+ letter + ".idx.part" + part));

			if (tm.firstKey().startsWith("")) {
				tm.remove(tm.firstKey());
			} else {
				letter = tm.firstKey().charAt(0);
			}

			for (Entry<String, Vector<Integer>> entry : tm.entrySet()) {
				String key = entry.getKey();
				if (key.charAt(0) == letter) {
					out = entry.getKey() + "\t" + entry.getValue().toString();
					os.write(out);
					os.write(newline);
				} else {
					os.close();
					letter = key.charAt(0);
					os = new BufferedWriter(new FileWriter("data/index/doc" + letter
							+ ".idx.part" + part));
					out = entry.getKey() + "\t" + entry.getValue().toString();
					os.write(out);
					os.write(newline);
				}
			}
			os.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// maybe we don't have to clear this
		_allDocs = new Vector<DocumentIndexed>();
		_documents = new HashMap<String, Vector<Integer>>();
	}

	public void loadFromFile(char c) {
		final String charName = String.valueOf(c).toLowerCase();
		if (_documents == null) {
			_documents = new HashMap<String, Vector<Integer>>();
		}
		String line = "";
		String[] map = { "" };
		String[] freqMap = { "" };
		Vector<Integer> temp;
		File file = new File("data/index/doc");

		// Filters files by name
		FilenameFilter textFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (name.startsWith(charName) && (name.contains(".idx"))) {
					return true;
				} else {
					return false;
				}
			}
		};
		// Sorts the files because listFiles returns the results backwards
		File[] sortedFiles = file.listFiles(textFilter);
		Arrays.sort(sortedFiles);
		try {
			// For all files of the name <code c>, enter in the information into
			// the hashmap
			for (File entry : sortedFiles) {
				BufferedReader reader = new BufferedReader(new FileReader(
						entry.getAbsoluteFile()));
				while ((line = reader.readLine()) != null) {
					map = line.split("\t");
					if (_documents.get(map[0]) != null) {
						temp = _documents.get(map[0]);
					} else {
						temp = new Vector<Integer>();
					}
					freqMap = map[1].substring(1, map[1].length() - 1).split(", ");
					for (int i = 0; i < freqMap.length; i++) {
						temp.add(Integer.parseInt(freqMap[i]));
					}
					_documents.put(map[0], temp);
				}
			}
		} catch (IOException ioe) {
			System.err.println("Oops " + ioe.getMessage());
		}

	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
	}

	@Override
	public Document getDoc(int docid) {
		// SearchEngine.Check(false, "Do NOT change, not used for this Indexer!");
		return _allDocs.get(docid);
	}

	/**
	 * In HW2, you should be using {@link DocumentIndexed}
	 */
	@Override
	public Document nextDoc(Query query, int docid) {
		Vector<Vector<Integer>> did = new Vector<Vector<Integer>>();
		query.processQuery();
		Vector<String> word = query._tokens;
		Vector<Integer> temp = new Vector<Integer>();
		int match = docid;
		int index = docid;
		boolean exist = false;
		/*
		 * This for loop first finds the vector in the hashmap that corresponds to
		 * the query word and then adds it to did
		 */
		for (int i = 0; i < word.size(); i++) {
			did.add(_documents.get(word.get(i)));
		}

		/*
		 * Assuming all documents are already in order (IE. 1,4,5 and not 4,5,1) K
		 * is all vectors after the first one
		 */
		temp = did.get(0);
		while (index < temp.size()) {
			index = temp.get(index) + 1;
			for (int k = 1; k < did.size(); k++) {
				if (did.get(k).contains(index)) {
					exist = true;
				} else {
					exist = false;
					k = 1;
					index = temp.get(temp.indexOf(index) + 1);
					// break; // does this break out of the for loop or break out of the
					// current k for the loop
				}
			}
			if (exist == true) {
				match = did.get(0).get(index);
				// terminates the while loop
				index = temp.size();
			}
			index++;
		}
		return _allDocs.get(match);
	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		// returns the size of the vector which contains the number of documents
		// that contain the term
		return _documents.get(term).size();
	}

	@Override
	public int corpusTermFrequency(String term) {
		// have to device a data structure to keep track of how many times the term
		// is used
		return _terms.get(term);
	}

	//
	@Override
	public int documentTermFrequency(String term, String url) {
		SearchEngine.Check(false, "Not implemented!");
		return 0;
	}

	public static String readToString(String fileName) {
		File file = new File(fileName);
		Long filelength = file.length();
		byte[] filecontent = new byte[filelength.intValue()];
		try {
			FileInputStream in = new FileInputStream(file);
			in.read(filecontent);
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new String(filecontent);
	}

	public static String Html2Text(String inputString) {
		String htmlStr = inputString;
		String textStr = "";
		java.util.regex.Pattern p_script;
		java.util.regex.Matcher m_script;
		java.util.regex.Pattern p_style;
		java.util.regex.Matcher m_style;
		java.util.regex.Pattern p_html;
		java.util.regex.Matcher m_html;

		try {
			String regEx_script = "<[\\s]*?script[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?script[\\s]*?>";
			String regEx_style = "<[\\s]*?style[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?style[\\s]*?>";
			String regEx_html = "<[^>]+>";

			p_script = Pattern.compile(regEx_script, Pattern.CASE_INSENSITIVE);
			m_script = p_script.matcher(htmlStr);
			htmlStr = m_script.replaceAll("");

			p_style = Pattern.compile(regEx_style, Pattern.CASE_INSENSITIVE);
			m_style = p_style.matcher(htmlStr);
			htmlStr = m_style.replaceAll("");

			p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
			m_html = p_html.matcher(htmlStr);
			htmlStr = m_html.replaceAll("");

			textStr = htmlStr;

		} catch (Exception e) {
			System.err.println("Html2Text: " + e.getMessage());
		}

		return textStr;
	}
}
