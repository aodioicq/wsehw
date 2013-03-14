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
public class IndexerInvertedOccurrence extends Indexer {

	private HashMap<String, Vector<Integer>> _freqOffset;
	public Vector<DocumentIndexed> _allDocs;
	private int numDoc;

	public IndexerInvertedOccurrence(Options options) {
		super(options);
		numDoc = 0;
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}

	private String stem(String word) {
		if (word.endsWith("s") && word.length() > 1) {
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
		if (word.endsWith("ed")) {
			word = word.substring(0, word.length() - 2);
			if (word.endsWith("at")) {
				word = word + "e";
			} else if (word.endsWith("bl")) {
				word = word + "e";
			} else if (word.endsWith("iz")) {
				word = word + "e";
			}
		}
		if (word.endsWith("ing")) {
			word = word.substring(0, word.length() - 3);
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
		//String corpusFile="data/wiki";
	    System.out.println("Construct index from: " + corpusFile);
	    
	    File root = new File(corpusFile);
        File[] files = root.listFiles();
        
        String constants=_options._indexPrefix+"/occurrences/constant.idx";
        BufferedWriter bw = new BufferedWriter(new FileWriter(constants,true));
        
		_freqOffset = new HashMap<String, Vector<Integer>>();
		_allDocs = new Vector<DocumentIndexed>();
		Vector<Integer> temp;

		int termOffset = 0;
		int did = 0;
		int didIndex = 0;
		int freq = 0;
		int freqIndex = 0;
		int corpusFreq = 0;
		int partStart = 0;
		int part = 1;
		for (int i = 0; i < files.length; i++) {
			termOffset = 0;
			String filename=corpusFile + "/"+files[i].getName();
			System.out.println("reading "+filename);
			//DocumentIndexed d = new DocumentIndexed(did);
			//_allDocs.add(d);
			int pos=0;
			String content=readToString(filename);
			content=Html2Text(content);
			Scanner s=new Scanner(content);
			while(s.hasNext()){
				String word=s.next();
				word=stem(word.toLowerCase());
				temp = new Vector<Integer>();
				if (!_freqOffset.containsKey(word)) {
					temp.add(did);
					temp.add(1);
					temp.add(termOffset);
				} else {

					temp = _freqOffset.get(word);

					didIndex = getCurrentDidIndex(did, temp);
					if (didIndex == -1) {
						// case where it is the first instance in a new document after
						// the initial indexing
						temp.add(did);
						temp.add(1);
						temp.add(termOffset);
					} else {
						// Updates the frequency and adds the offset
						freqIndex = didIndex + 1;
						freq = temp.get(freqIndex);
						freq++;
						temp.set(freqIndex, freq);
						temp.add(termOffset);
					}
				}
				termOffset++;
				_freqOffset.put(word, temp);
			}
			int bodysize=termOffset+1;
			bw.write(did+"\t"+files[i].getName()+"\t"+bodysize);
			bw.newLine();
			bw.flush();
		
			if(did>part*200){
				saveToFile(part);
				part++;
				_freqOffset.clear();
			}
			corpusFreq+= bodysize;
			
			
			did++;
		}
		saveToFile(part);
		// Stores the corpus term frequency to file
		File corpusIndex = new File(_options._indexPrefix+"/occurrences/frequency");
		if (!corpusIndex.exists()) {
			corpusIndex.mkdir();
		}
		File corpusTerms = new File(_options._indexPrefix+"/occurrences/frequency/corpusterms.idx");
		BufferedWriter os = new BufferedWriter(new FileWriter(corpusTerms));
		os.write(Integer.toString(corpusFreq));
		os.close();
	}

	public void saveToFile(int part) {
		String newline = System.getProperty("line.separator");
		char letter = 'a';
		String out = " ";
		TreeMap<String, Vector<Integer>> tm = new TreeMap<String, Vector<Integer>>(
				_freqOffset);
		//String prefix=_options._indexPrefix+"/occurrences";
		String prefix="data/index/occurrences";
		File f = new File(prefix);
		if (!f.exists()) {
			f.mkdir();
		}
		try {

			if (tm.firstKey().startsWith("")) {
				tm.remove(tm.firstKey());
			} 
			char a=tm.firstKey().charAt(0);
			//System.out.println(a);
			while(!Character.isLetter(a)){
				tm.remove(tm.firstKey());	
				a=tm.firstKey().charAt(0);
			}
			letter = tm.firstKey().charAt(0);
			//System.out.println(letter);
			BufferedWriter os = new BufferedWriter(new FileWriter(prefix+"/"
					+ letter + ".idx.part" + part));

			for (Entry<String, Vector<Integer>> entry : tm.entrySet()) {
				String key = entry.getKey();
				if (key.charAt(0) == letter) {
					out = entry.getKey() + "\t" + entry.getValue().toString();
					os.write(out);
					os.write(newline);
				} else {
					letter = key.charAt(0);
					if(Character.isLetter(letter)){
						os = new BufferedWriter(new FileWriter(prefix+"/"
								+ letter + ".idx.part" + part));
						out = entry.getKey() + "\t" + entry.getValue().toString();
						os.write(out);
						os.write(newline);
					}
				}
			}
			os.flush();
			os.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// maybe we don't have to clear this
		// _allDocs = new Vector<DocumentIndexed>();
		//_freqOffset = new HashMap<String, Vector<Integer>>();
	}

	public void loadFromFile(char c) {
		final String charName = String.valueOf(c).toLowerCase();
		if (_freqOffset == null) {
			_freqOffset = new HashMap<String, Vector<Integer>>();
		}
		String line = "";
		String[] map = { "" };
		String[] freqMap = { "" };
		Vector<Integer> temp;
		File file = new File("data/index/occurrences");

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
					if (_freqOffset.get(map[0]) != null) {
						temp = _freqOffset.get(map[0]);
					} else {
						temp = new Vector<Integer>();
					}
					freqMap = map[1].substring(1, map[1].length() - 1).split(
							", ");
					for (int i = 0; i < freqMap.length; i++) {
						temp.add(Integer.parseInt(freqMap[i]));
					}
					_freqOffset.put(map[0], temp);
				}
			}
		} catch (IOException ioe) {
			System.err.println("Oops " + ioe.getMessage());
		}

	}

	public Vector<Integer> getOffsets(Vector<Integer> did, int docid) {
		Vector<Integer> temp = new Vector<Integer>();
		int didIndex = 0;
		int freqIndex = 0;
		int freq = 0;

		didIndex = getCurrentDidIndex(docid, did);
		freqIndex = didIndex + 1;
		freq = did.get(freqIndex);
		for (int i = 1; i <= freq; i++) {
			temp.add(did.get(freqIndex + i));
		}
		return temp;
	}

	// Assuming there is only one set of offsets at a time
	public boolean checkPhrase(Vector<Vector<Integer>> offsets) {
		Vector<Integer> first = offsets.get(0);
		// contains is for more than 1 word in the phrase
		Boolean contains = false;

		for (int i = 0; i < first.size(); i++) {
			for (int j = 1; j < offsets.size(); j++) {
				if (offsets.get(j).contains(first.get(i) + j)) {
					contains = true;

				}
			}
			if (contains == true) {
				return contains;
			}
		}
		// System.out.println(contains);
		return contains;
	}

	public Vector<Integer> getMatches(Vector<Vector<Integer>> didOnly,
			Vector<Vector<Integer>> didOriginal) {
		Vector<Integer> temp = new Vector<Integer>();
		Vector<Integer> phrase = new Vector<Integer>();
		Vector<Vector<Integer>> offsets;
		Vector<Integer> phraseVector = new Vector<Integer>();
		Vector<Vector<Integer>> compareFirst = new Vector<Vector<Integer>>();
		boolean exist = false;
		boolean isPhrase = false;

		temp = didOnly.get(0);

		for (int j = 0; j < temp.size(); j++) {
			for (int k = 1; k < didOnly.size(); k++) {
				if (didOnly.get(k).contains(temp.get(j))) {

					exist = true;
				} else {
					// check next did
					k = 1;
					exist = false;
				}
			}
			if (exist == true) {
				phrase.add(temp.get(j));
			}
		}
		// Gets the offsets of all words in the phrase
		for (int k = 0; k < phrase.size(); k++) {
			offsets = new Vector<Vector<Integer>>();
			for (int i = 0; i < didOriginal.size(); i++) {
				offsets.add(getOffsets(didOriginal.get(i), phrase.get(k)));

			}

			isPhrase = checkPhrase(offsets);

			if (isPhrase == true) {

				phraseVector.add(phrase.get(k));
				offsets.remove(0);
			} else {
				// resets the offsets
				offsets.remove(0);
			}
		}
		return phraseVector;

	}

	public Vector<Integer> getPhraseVector(String[] queryPhrase) {

		Vector<Integer> phraseMatch = new Vector<Integer>();

		Vector<Vector<Integer>> didOnly = new Vector<Vector<Integer>>();
		Vector<Vector<Integer>> didOriginal = new Vector<Vector<Integer>>();
		// Gets the vector for each word
		for (int i = 0; i < queryPhrase.length; i++) {
			didOriginal.add(_freqOffset.get(queryPhrase[i]));
		}

		didOnly = getOnlyDid(didOriginal);

		phraseMatch = getMatches(didOnly, didOriginal);

		return phraseMatch;
	}

	/**
	 * This method gets the did for the case where the index information is
	 * reset due to adding a different word.
	 * 
	 * @param did
	 * @param temp
	 * @return
	 */
	public Integer getCurrentDidIndex(int did, Vector<Integer> temp) {
		int tempDid = 0;
		int didIndex = 0;
		int freq = 0;
		int freqIndex = 0;

		while (tempDid != did) {

			tempDid = temp.get(didIndex);
			freqIndex = didIndex + 1;
			freq = temp.get(freqIndex);

			if (tempDid != did) {
				didIndex = freqIndex + freq + 1;
				freqIndex = didIndex + 1;
				if (didIndex > temp.size() - 1) {
					return -1;
				}
				freq = temp.get(freqIndex);
				tempDid = temp.get(didIndex);
			}
		}
		return didIndex;
	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		String constantFile = _options._indexPrefix + "/occurance/constant.idx";
		BufferedReader reader = new BufferedReader(new FileReader(constantFile));
	    try {
	      String line = null;
	      while ((line = reader.readLine()) != null) {
	    	  Scanner s = new Scanner(line).useDelimiter("\t");
	    	  int id=Integer.parseInt(s.next());
	    	  String url=s.next();
	    	  int bodySize=Integer.parseInt(s.next());
	    	  DocumentIndexed di=new DocumentIndexed(id);
	    	  di.setUrl(url);
	    	  di.bodySize=bodySize;
	    	  _allDocs.add(di);
	      }
	    } finally {
	      reader.close();
	    }
	}

	@Override
	public Document getDoc(int docid) {
		SearchEngine.Check(false, "Do NOT change, not used for this Indexer!");
		return null;
	}

	public Integer next(int did, Vector<Integer> temp) {
		int nextDid = 0;
		int didIndex = 0;
		int freq = 0;
		int freqIndex = 0;

		// Gets the current did index
		didIndex = getCurrentDidIndex(temp.get(did), temp);
		freqIndex = didIndex + 1;
		freq = temp.get(freqIndex);
		nextDid = freq + freqIndex + 1;
		if (nextDid >= temp.size() - 1) {
			return -1;
		}
		// this should return the index of the next did
		return nextDid;
	}

	public Vector<Vector<Integer>> getOnlyDid(Vector<Vector<Integer>> allDid) {
		Vector<Integer> tempDidVector;
		int tempDid = 0;
		Vector<Vector<Integer>> did = new Vector<Vector<Integer>>();
		// Loops through allDid
		for (int i = 0; i < allDid.size(); i++) {
			tempDidVector = new Vector<Integer>();
			// For each vector, remove all integers that is not a did
			while (tempDid != -1) {
				// Adds each did index to the tempDidVector. -1 means that it
				// reached
				// the end of the vector.;
				tempDidVector.add(allDid.get(i).get(tempDid));
				tempDid = next(tempDid, allDid.get(i));
			}
			tempDid = 0;
			did.add(tempDidVector);
		}
		return did;
	}

	/**
	 * In HW2, you should be using {@link DocumentIndexed}.
	 * 
	 * This nextDoc is similar to IndexerInvertedDoconly's nextDoc with the
	 * exception of the format of the data. nextDoc formats the vector to
	 * contain just the did's
	 */
	@Override
	public Document nextDoc(Query query, int docid) {
		Vector<Vector<Integer>> tempDid = new Vector<Vector<Integer>>();
		Vector<Vector<Integer>> did = new Vector<Vector<Integer>>();
		query.processQuery();
		Vector<String> word = query._tokens;
		Vector<Integer> temp = new Vector<Integer>();
		int match = docid;
		int index = 0;
		int tempIndex = docid;
		boolean exist = false;
		String[] removePhrase = {};
		String [] checkPhrase = {};
		/*
		 * This for loop first finds the vector in the hashmap that corresponds to
		 * the query word and then adds it to did
		 */
		
		for (int i = 0; i < word.size(); i++) {
		
			// loads from the related files
				if(word.get(i).contains(" ") == true) {
					checkPhrase = word.get(i).split(" ");
						for(int j = 0;j<checkPhrase.length;j++) {
								loadFromFile(checkPhrase[j].charAt(0));
							}
						if(!getPhraseVector(checkPhrase).isEmpty()) {
						tempDid.add(getPhraseVector(checkPhrase)); 
						} else {
							return new DocumentIndexed(Integer.MAX_VALUE);
						}
					}else {
							loadFromFile(word.get(i).charAt(0));
							tempDid.add(_freqOffset.get(word.get(i)));
						}
		}
		did = getOnlyDid(tempDid);
		// Assuming only one did vector which would mean only one query word
		if (did.size() > 1) {
			temp = did.get(0);

			index = temp.indexOf(docid) + 1;
			for (int i = index; i < temp.size(); i++) {
				for (int k = 1; k < did.size(); k++) {
					if (did.get(k).contains(temp.get(index))) {
						exist = true;
					} else {
						// check next did
						k = 1;
						exist = false;
					}
				}
				if (exist == true) {
					match = did.get(0).get(index);

					// terminates the while loop
					index = temp.size();
				}
				index++;
			}
		} else {
			// the case for one query word which results in one did so get the
			// next did
			if (did.indexOf(docid) != did.size() - 1) {
				int curIndex = did.get(0).indexOf(docid);
				match = did.get(0).get(curIndex + 1);

			}
		}
		if (match == docid) {
			return new DocumentIndexed(Integer.MAX_VALUE);
		} else {
			// updates the term frequency in the document

			// Flesh this out. _All docs is cleared after saving so maybe create
			// a new Document? However, it will not retain any prior information
			DocumentIndexed tempDI = new DocumentIndexed(match);

			for (int i = 0; i < word.size(); i++) {
				// I believe it is match that is the current document id
				tempDI.bodySize = getDocumentBodySize(match);
				tempDI.documentTermFrequency.add(documentTermFrequency(word.get(i),
						match));
			}
			clear();
			return tempDI;
		}
		
	}
	public int getDocumentBodySize(int docid) {
		String posting_list = "INSERT FILE LOCATION";
		String line = " ";
		String[] word = {};
		int bodysize = 0;
		try {
			BufferedReader in = new BufferedReader(new FileReader(posting_list));
			// do we want to store it or just read
			while((line = in.readLine()) != null) {
			word = line.split("\t");
			// url did bodysize 
				if(Integer.parseInt(word[1]) == docid) {
					bodysize = Integer.parseInt(word[3]);
					break;
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bodysize;
	}
	public void clear() {
		_freqOffset.clear();
	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
			loadFromFile(term.charAt(0));
		// creates vector of vector of integers to reuse a helper method
		Vector<Vector<Integer>> termVector = new Vector<Vector<Integer>>();
		Vector<Vector<Integer>> temp = new Vector<Vector<Integer>>();
		int numDoc = 0;

		termVector.add(_freqOffset.get(term));
		temp = getOnlyDid(termVector);
		numDoc = temp.get(0).size();
		clear();
		return numDoc;
	}

	@Override
	public int corpusTermFrequency(String term) {
			loadFromFile(term.charAt(0));
		// creates vector of vector of integers to reuse a helper method
		Vector<Vector<Integer>> termVector = new Vector<Vector<Integer>>();
		Vector<Vector<Integer>> temp = new Vector<Vector<Integer>>();
		int numTerm = 0;
		int freqIndex = 0;
		termVector.add(_freqOffset.get(term));
		// Gets the vector of did
		temp = getOnlyDid(termVector);

		// adds up the frequency of the term in all documents
		for (int i = 0; i < temp.get(0).size(); i++) {
			// gets all freq index using the did + 1
			freqIndex = temp.get(0).get(i) + 1;
			numTerm += _freqOffset.get(term).indexOf(freqIndex);
		}
		clear();
		return numTerm;
	}

	// Returns the number of instances of the query in the document
	public int documentTermFrequency(String term, int docid) {
		Vector<Vector<Integer>> termVector = new Vector<Vector<Integer>>();
		Vector<Vector<Integer>> temp = new Vector<Vector<Integer>>();
		int didIndex = 0;
		int freqIndex = 0;
		termVector.add(_freqOffset.get(term));
		// Gets the vector of did
		didIndex = getCurrentDidIndex(docid, termVector.get(0));
		freqIndex = didIndex + 1;
		return _freqOffset.get(term).get(freqIndex);
	}

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
            String textStr =""; 
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
      
          p_script = Pattern.compile(regEx_script,Pattern.CASE_INSENSITIVE); 
          m_script = p_script.matcher(htmlStr); 
          htmlStr = m_script.replaceAll(""); 

          p_style = Pattern.compile(regEx_style,Pattern.CASE_INSENSITIVE); 
          m_style = p_style.matcher(htmlStr); 
          htmlStr = m_style.replaceAll(""); 
      
          p_html = Pattern.compile(regEx_html,Pattern.CASE_INSENSITIVE); 
          m_html = p_html.matcher(htmlStr); 
          htmlStr = m_html.replaceAll(""); 
      
       textStr = htmlStr; 
      
      }catch(Exception e) { 
               System.err.println("Html2Text: " + e.getMessage()); 
      } 
   
      return textStr;
      }   
  
}
