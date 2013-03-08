package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

public class RankerQL extends Ranker {
	public RankerQL(Options options,
		      CgiArguments arguments, Indexer indexer) {
		    super(options, arguments, indexer);
		    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
		  }
	
	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
		 Vector<ScoredDocument> all = new Vector<ScoredDocument>();
		    for (int i = 0; i < _indexer.numDocs(); ++i) {
		      all.add(scoreDocument(query, i));
		    }
		    Collections.sort(all, Collections.reverseOrder());
		    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
		    for (int i = 0; i < all.size() && i < numResults; ++i) {
		      results.add(all.get(i));
		    }
		    return results;
	}
	
	public ScoredDocument scoreDocument(Query query, int did) {
		query.processQuery();
		Vector<String> qv=query._tokens;
	    // Get the document tokens.
	    Document d = _indexer.getDoc(did);
	    Vector<String> db = ((DocumentFull) d).getConvertedBodyTokens();  
	    Vector<String> dv = ((DocumentFull) d).getConvertedTitleTokens();  

		// Stores all text in the document body and maps the frequency to it
		int db_size = db.size();
		long col_size = _indexer.totalTermFrequency();
		Vector<Double> total_freq = new Vector<Double>();
		Vector<Double> dbXqv_freq = new Vector<Double>();
		Vector<Double> db_freq = new Vector<Double>();

		Vector<String> all = new Vector<String>();
		String key = "";
		double score = 1;
		double constant = .50;
		int index = 0;

		all = populateAll(db, all);
		db_freq = initializeVector(all.size());

		for (int k = 0; k < db.size(); k++) {
			index = all.indexOf(db.get(k));
			db_freq.set(index, db_freq.get(index) + 1);
		}
		for (int j = 0; j < qv.size(); j++) {
			if (all.contains(qv.get(j))) {
				dbXqv_freq.add((double) db_freq.get(all.indexOf(qv.get(j)))
						/ db_size); // error checking for if it does not exist
			} else {
				dbXqv_freq.add(0.0);
			}
			//System.out.print("Document Body frequency: " + dbXqv_freq.get(j));
			total_freq.add((double) _indexer.corpusTermFrequency(qv.get(j)) / col_size);
			//System.out.print("  |  Collection frequency: "
			// + d.termFrequency(qv.get(j)));
			//System.out.println("");

		}
		for (int l = 0; l < dbXqv_freq.size(); l++) {
			score = score
					* ((constant * dbXqv_freq.get(l)) + ((1 - constant) * total_freq
							.get(0)));
		}
		//System.out.println("The score is: " + score);

		return new ScoredDocument(d, score);
	}
	private Vector<String> populateAll(Vector<String> dbXqv, Vector<String> all) {
		String key = "";
		for (int i = 0; i < dbXqv.size(); i++) {
			key = dbXqv.get(i);
			if (!all.contains(key)) {
				all.add(key);
			}

		}
		return all;
	}
  
  private Vector<Double> initializeVector(int size) {
		Vector<Double> db_freq = new Vector<Double>();
		for (int z = 0; z < size; z++) {
			db_freq.add(0.0);
		}
		return db_freq;

	}

}
