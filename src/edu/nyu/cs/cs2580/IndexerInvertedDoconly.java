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
public class IndexerInvertedDoconly extends Indexer {

	private static HashMap<String, Vector<Integer>> _documents;
	private static HashMap<String, Integer> _terms;
	 public Vector < DocumentIndexed > _allDocs;
	 private String index_source;

	public IndexerInvertedDoconly(Options options) {
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
		_documents = new HashMap<String, Vector<Integer>>();
  	_allDocs = new Vector <DocumentIndexed>();
  	_terms = new HashMap <String,Integer>();
		index_source = "/";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(index_source));
			try {
				String line = null;
				String[] word = null;
				Vector<Integer> id = new Vector<Integer>();
				int did = 0;
				String stemmed = "";
				while ((line = reader.readLine()) != null) {
					// each document is a single line
					DocumentIndexed d = new DocumentIndexed(did);
					_allDocs.add(d);
       
					word = line.split("[ .,?!]+");
					for (int i = 0; i < word.length; i++) {
						/*
						 * CHECK THIS LOGIC FOR NON-WORDS SUCH AS COMMAS AND ETC
						 */
						
						// If the hashmap has the word as a key, check to see if the did exists in the vector
						// resets the vector for each word as well as cases where the word is first entered as a key
						id.clear();
						stemmed = stem(word[i]);
						if(!word[i].startsWith("<")) {
						if(_documents.containsKey(word[i]))
						{
						_terms.put(word[i],_terms.get(word[i])+1);
						id = _documents.get(word[i]);
						if (!id.contains(did)) {
							id.add(did);
						}
						_documents.put(word[i], id);
						}
						else {
							// otherwise, add the did to the vector for the _documennts as well as the _terms
							id.add(did);
							_documents.put(word[i],id);
							_terms.put(word[i], 1);
						}
					}
					did++;
					}
				}
			} finally {
				reader.close();
			}
		} catch (IOException ioe) {
			System.err.println("Oops " + ioe.getMessage());
		}
		System.out.println("Done indexing " + Integer.toString(_documents.size())
				+ " documents...");
	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
	}

	@Override
	public Document getDoc(int docid) {
	//	SearchEngine.Check(false, "Do NOT change, not used for this Indexer!");
		return _allDocs.get(docid);
	}

	/**
	 * In HW2, you should be using {@link DocumentIndexed}
	 */
	@Override
	public Document nextDoc(Query query, int docid) {
		Vector < Vector < Integer > > did = new Vector < Vector < Integer > >();
		query.processQuery();
		Vector < String > word = query._tokens;
		Vector < Integer > temp = new Vector < Integer >();
		int match = docid;
		int index = docid;
		boolean exist = false;
		/*
		 * This for loop first finds the vector in the hashmap 
		 * that corresponds to the query word and then adds it to did
		 */
		for (int i = 0; i < word.size(); i++)
		{
			did.add(_documents.get(word.get(i)));
		}
		
		/*
		 * Assuming all documents are already in order (IE. 1,4,5 and not 4,5,1)
		 * K is all vectors after the first one
		 */
		temp = did.get(0);
		while (index<temp.size())
		{
			index = temp.get(index)+1;
			for ( int k = 1; k < did.size();k++)
			{
				if(did.get(k).contains(index))
				{
					exist = true;
				}
				else {
					exist = false;
					k = 1;
					index = temp.get(temp.indexOf(index)+1);
					//break;	// does this break out of the for loop or break out of the current k for the loop
				}
			}
			if(exist == true)
			{
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
// returns the size of the vector which contains the number of documents that contain the term
		return _documents.get(term).size();
	}

	@Override
	public int corpusTermFrequency(String term) {
		// have to device a data structure to keep track of how many times the term is used
		return _terms.get(term);
	}
//
	@Override
	public int documentTermFrequency(String term, String url) {
		SearchEngine.Check(false, "Not implemented!");
		return 0;
	}
}
