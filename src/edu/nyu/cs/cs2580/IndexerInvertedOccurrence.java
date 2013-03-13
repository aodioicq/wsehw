package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer {

	private static HashMap<String, Vector<Integer>> _freqOffset;
	public Vector<DocumentIndexed> _allDocs;
	private String index_source;

	public IndexerInvertedOccurrence(Options options) {
		super(options);
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
		_freqOffset = new HashMap<String, Vector<Integer>>();
		_allDocs = new Vector<DocumentIndexed>();
		index_source = "data/simple/test.txt";
		String line = null;
		String[] word = null;
		String[] splitDoc = null;
		Vector<Integer> temp;

		int termOffset = 0;
		int did = 0;
		int didIndex = 0;
		int freq = 0;
		int freqIndex = 0;
		
		int partStart = 0;
		int part = 1;
		boolean code = false;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(index_source));
			try {	
			

				while ((line = reader.readLine()) != null) {
					// each document is in it's own line
					DocumentIndexed d = new DocumentIndexed(did);
					_allDocs.add(d);
					//splitDoc = line.split("\t");
					//word = splitDoc[1].split("[ .,?!]+");
					// For my own test file
					 word = line.split("[ .,?!]+");
					for (int i = 0; i < word.length; i++) {
						// Gets the location of the current word in the document
						word[i] = stem(word[i].toLowerCase());
						// creates a new temporary vector for every new word
						temp = new Vector<Integer>();
						 if(!word[i].startsWith("<") && code == false) {
						// Very first instance where the word is entered into the map
						if (!_freqOffset.containsKey(word[i])) {
							temp.add(did);
							temp.add(1);
							temp.add(termOffset);
						} else {

							temp = _freqOffset.get(word[i]);

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
						termOffset = i + 1;
						_freqOffset.put(word[i], temp);
					}
						 else {
						// Checks to make sure the word is not non-visible page content such
						// as <Script>
								code = true;
								if(word[i].endsWith(">")) {
									code = false;
								}
						 }
					} 
					// Somehow need to save the bodysize
					d.bodySize = termOffset + 1;
					did++;
					// Splits off to avoid memory limitations
					if(did==partStart+3) {
						partStart = did+1;
						saveToFile(part);
						part++;
					}
					termOffset = 0;
				}
				saveToFile(part);
				
			} finally {
				reader.close();
			}
		} catch (IOException ioe) {
			System.err.println("Oops " + ioe.getMessage());
		}
	}
	public void saveToFile(int part) {
		String newline = System.getProperty("line.separator");
		char letter = 'a';
		String out = " ";
		TreeMap<String, Vector<Integer>> tm = new TreeMap<String, Vector<Integer>>(
				_freqOffset);
		File f = new File("data/index/");
		if(!f.exists()) {
			f.mkdir();
		}
		try {
		
			BufferedWriter os = new BufferedWriter(new FileWriter("data/index/"
					+ letter + ".idx.part" + part));
		
			if(tm.firstKey().startsWith(""))
			{
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
					os = new BufferedWriter(new FileWriter("data/index/" + letter
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
		_allDocs = new Vector<DocumentIndexed>();
		_freqOffset = new HashMap<String,Vector<Integer>>();
	}

	public void loadFromFile(char c) {
		final String charName = String.valueOf(c).toLowerCase();
		if(_freqOffset == null) {
		_freqOffset = new HashMap<String, Vector<Integer>>();
		}
		String line = "";
		String[] map = { "" };
		String[] freqMap = { "" };
		Vector<Integer> temp;
		File file = new File("data/index/");
		
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
			// For all files of the name <code c>, enter in the information into the hashmap
			for(File entry : sortedFiles)
			{
			BufferedReader reader = new BufferedReader(new FileReader(entry.getAbsoluteFile()));
			while ((line = reader.readLine()) != null) {
				map = line.split("\t");
				if(_freqOffset.get(map[0]) != null) {
				temp = _freqOffset.get(map[0]);
				} else {
					temp = new Vector<Integer>();
				}
				freqMap = map[1].substring(1, map[1].length() - 1).split(", ");
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

	
	public Vector<Integer> getOffsets(Vector<Integer> did,int docid) {
		Vector<Integer> temp = new Vector<Integer>();
		int didIndex = 0;
		int freqIndex = 0;
		int freq = 0;
	
		didIndex = getCurrentDidIndex(docid,did);
		freqIndex = didIndex+1;
		freq = did.get(freqIndex);
		for(int i = 1;i<=freq;i++) {
			temp.add(did.get(freqIndex+i));
		}
		return temp;
	}
	// Assuming there is only one set of offsets at a time
	public boolean checkPhrase(Vector<Vector<Integer>> offsets) {
		Vector<Integer> first = offsets.get(0);
		// contains is for more than 1 word in the phrase
		Boolean contains = false;

		for(int i = 0;i<first.size();i++) {
			for(int j = 1;j<offsets.size();j++) {
				if(offsets.get(j).contains(first.get(i)+j)) {
					contains = true;
					
				} 
			}
			if(contains == true) {
				return contains;
			}
			}
	//	System.out.println(contains);
		return contains;
		}
		
	
	public Vector <Integer> getMatches(Vector<Vector<Integer>> didOnly,Vector<Vector<Integer>> didOriginal) {
		Vector<Integer> temp = new Vector<Integer>();
		Vector<Integer> phrase = new Vector<Integer>();
		Vector<Vector<Integer>> offsets;
		Vector<Integer> phraseVector = new Vector<Integer>();
		Vector<Vector<Integer>> compareFirst = new Vector<Vector<Integer>>();
		boolean exist = false;
		boolean isPhrase = false;
		
		temp = didOnly.get(0);
		
		for(int j = 0;j<temp.size();j++) {
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
	for(int k = 0;k<phrase.size();k++) {
		offsets = new Vector<Vector<Integer>>();
		for(int i = 0;i<didOriginal.size();i++) {
			offsets.add(getOffsets(didOriginal.get(i),phrase.get(k)));

		}
		
		isPhrase = checkPhrase(offsets);
	
		if(isPhrase == true) {
		
			phraseVector.add(phrase.get(k));
			offsets.remove(0);
		}
		else {
			//resets the offsets
			offsets.remove(0);
		}
	}
		return phraseVector;
		
	}
	public Vector <Integer> getPhraseVector(String[] queryPhrase) {
	
		Vector<Integer> phraseMatch = new Vector<Integer>();
		
		Vector<Vector<Integer>> didOnly = new Vector<Vector<Integer>>();
		Vector<Vector<Integer>> didOriginal = new Vector<Vector<Integer>>();
		// Gets the vector for each word
		for(int i = 0;i<queryPhrase.length;i++) {
			didOriginal.add(_freqOffset.get(queryPhrase[i]));
		}
		
			didOnly = getOnlyDid(didOriginal);

			phraseMatch = getMatches(didOnly,didOriginal);

		return phraseMatch;
	}
	/**
	 * This method gets the did for the case where the index information is reset
	 * due to adding a different word.
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
		if (nextDid >= temp.size()-1) {
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
				// Adds each did index to the tempDidVector. -1 means that it reached
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
	 * exception of the format of the data. nextDoc formats the vector to contain
	 * just the did's
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
		/*
		 * This for loop first finds the vector in the hashmap that corresponds to
		 * the query word and then adds it to did
		 */	
		for (int i = 0; i < word.size(); i++) {
			tempDid.add(_freqOffset.get(word.get(i)));
		}
		did = getOnlyDid(tempDid);
		// Assuming only one did vector which would mean only one query word
		if(did.size() > 1)
		{
		temp = did.get(0);

			index = temp.indexOf(docid) + 1;
	for (int i = index;i<temp.size();i++){
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
		}
		else { 
			// the case for one query word which results in one did so get the next did
			if(did.indexOf(docid) != did.size()-1) {
				int curIndex = did.get(0).indexOf(docid);
			match = did.get(0).get(curIndex+1);
		
			}
		}
		if(match == docid) {
			return new DocumentIndexed(Integer.MAX_VALUE);
		} else {
			// updates the term frequency in the document
		
			// Flesh this out. _All docs is cleared after saving so maybe create a new Document? However, it will not retain any prior information
			DocumentIndexed tempDI = new DocumentIndexed(match);
		
			for (int i = 0;i<word.size();i++) {
			// I believe it is match that is the current document id
			tempDI.documentTermFrequency.add(documentTermFrequency(word.get(i),match));
			}
		return tempDI;
		}
	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		// creates vector of vector of integers to reuse a helper method
				Vector<Vector<Integer>> termVector = new Vector<Vector<Integer>>();
				Vector<Vector<Integer>> temp = new Vector<Vector<Integer>>();
				int numDoc = 0;

				termVector.add(_freqOffset.get(term));
				temp = getOnlyDid(termVector);
				numDoc = temp.get(0).size();
				return numDoc;
	}

	@Override
	public int corpusTermFrequency(String term) {
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
		return numTerm;
	}
	// Returns the number of instances of the query in the document
	public int documentTermFrequency(String term,int docid) {
		Vector<Vector<Integer>> termVector = new Vector<Vector<Integer>>();
		Vector<Vector<Integer>> temp = new Vector<Vector<Integer>>();
		int didIndex = 0;
		int freqIndex = 0;
		termVector.add(_freqOffset.get(term));
		// Gets the vector of did
		didIndex = getCurrentDidIndex(docid,termVector.get(0));
		freqIndex = didIndex+1;
		return _freqOffset.get(term).get(freqIndex);
	}
	@Override
	public int documentTermFrequency(String term, String url) {
		SearchEngine.Check(false, "Not implemented!");
		return 0;
	}
}
