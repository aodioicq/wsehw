package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

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
		if(word.endsWith("s")) {
			if (word.endsWith("sses")) {
				word = word.substring(0, word.length()-2);
			} else if (word.endsWith("ies")) {
				word = word.substring(0, word.length()-2);
			} else {
				word = word.substring(0, word.length()-1);
			}
		} 
		if (word.endsWith("eed")) {
			
		}
		if(word.endsWith("edly") || word.endsWith("ingly")) {
			word = word.substring(0, word.length()-2);
			
		} 
		if(word.endsWith("ed") || word.endsWith("ly")) {
			word = word.substring(0, word.length()-2);
			if(word.endsWith("at")) {
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
		index_source = "/";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(index_source));
			try {
				String line = null;
				String[] word = null;
				Vector<Integer> temp = new Vector<Integer>();

				int termOffset = 0;
				int did = 0;
				int didIndex = 0;
				int freq = 0;
				int freqIndex = 0;
				String stemmed = "";

				while ((line = reader.readLine()) != null) {
					// each document is in it's own line
					DocumentIndexed d = new DocumentIndexed(did);
					_allDocs.add(d);
					word = line.split("[ .,?!]+");

					for (int i = 0; i < word.length; i++) {
						// Gets the location of the current word in the document
						// Checks to make sure the word is not non-visible page content such as <Script>
						if(!word[i].startsWith("<")) {
						termOffset = i + 1;
						stemmed = stem(word[i]);
						/*
						 * CHECK THIS LOGIC FOR NON-WORDS SUCH AS COMMAS AND ETC maybe edit
						 * split line.split("[ .,?!]+"); <- PROBABLY IMPLEMENT THIS
						 */

						/*
						 * Checks to see if the map contains the word. If it does not,
						 * initialize an integer vector with the document id, frequency of
						 * 1, and the term offset. Otherwise, update the vector of the word1
						 * 2 15 29 2 3 5 22 54 is an example of 2 documents. Doc 1 has 2
						 * terms at offset 15 and 29. Doc 2 has 3 terms at offset 5, 22, and
						 * 54
						 */
						// Very first instance where the word is entered into the map
						if (!_freqOffset.containsKey(word[i])) {
							temp.add(did);
							temp.add(1);
							temp.add(termOffset);
						} else {
							temp = _freqOffset.get(word[i]);
							didIndex = getCurrentDidIndex(did, temp);
							if (didIndex == -1) {
								// case where it is the first instance in a new document
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
						_freqOffset.put(word[i], temp);
					}
					d.bodySize = termOffset+1;
					did++;
					termOffset = 0;
					}
				}
			} finally {
				reader.close();
			}
		} catch (IOException ioe) {
			System.err.println("Oops " + ioe.getMessage());
		}
		// System.out.println("Done indexing " + Integer.toString(_documents.size())
		// + " documents...");
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
				didIndex += freqIndex + freq + 1;
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
		didIndex = getCurrentDidIndex(did, temp);
		freqIndex = didIndex + 1;
		freq = temp.get(freqIndex);
		nextDid = freq + freqIndex + 1;
		if (nextDid > temp.size()) {
			return -1;
		}
		return nextDid;
	}

	public Vector<Vector<Integer>> getOnlyDid(Vector<Vector<Integer>> allDid) {
		Vector<Integer> tempDidVector = new Vector<Integer>();
		int tempDid = 0;
		Vector<Vector<Integer>> did = new Vector<Vector<Integer>>();

		// Loops through allDid
		for (int i = 0; i < allDid.size(); i++) {
			// For each vector, remove all integers that is not a did
			while (tempDid != -1) {
				// Adds each did index to the tempDidVector. -1 means that it reached
				// the end of the vector.
				tempDidVector.add(tempDid);
				tempDid = next(tempDid, allDid.get(i));
			}
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
			tempDid.add(_freqOffset.get(word.get(i)));
			
		}
		did = getOnlyDid(tempDid);
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
		if(match == docid) {
			return new DocumentIndexed(Integer.MAX_VALUE);
		} else {
			// updates the term frequency in the document
			DocumentIndexed tempDI = _allDocs.get(match);
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
			numTerm += _freqOffset.get(term).get(freqIndex);
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
		temp = getOnlyDid(termVector);
		didIndex = temp.get(0).indexOf(docid);  
		freqIndex = didIndex+1;
		return _freqOffset.get(term).get(freqIndex);
	}
	@Override
	public int documentTermFrequency(String term, String url) {
		SearchEngine.Check(false, "Not implemented!");
		return 0;
	}
}
