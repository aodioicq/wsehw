package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

public class RankerCosine extends Ranker {

  public RankerCosine(Options options,
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
  
  // refactored from cosine
  public ScoredDocument scoreDocument(Query query, int did) {

	  	query.processQuery();
	  	Vector<String> qv=query._tokens;
	    // Get the document tokens.
	    Document doc = _indexer.getDoc(did);
	    Vector<String> db = ((DocumentFull) doc).getConvertedBodyTokens();  
	    Vector<String> dv = ((DocumentFull) doc).getConvertedTitleTokens();  

		// Stores all text in the document body and maps the frequency to it
		Vector<String> all = new Vector<String>();
		Vector<Double> db_freq = new Vector<Double>();
		Vector<Double> qv_freq = new Vector<Double>();
		double score;
		String key = "";
		int index = 0;

		// Adds all elements in the document body and the query into one vector
		// need to do this for qv and db
		all = populateAll(db, all);
		all = populateAll(qv, all);
		all = populateAll(dv, all);
		db_freq = initializeVector(all.size());
		qv_freq = initializeVector(all.size());

		// Increments the frequency vectors where there are matches ////////
		for (int k = 0; k < db.size(); k++) {
			index = all.indexOf(db.get(k));
			db_freq.set(index, db_freq.get(index) + 1);
		}

		for (int l = 0; l < qv.size(); l++) {
			index = all.indexOf(qv.get(l));
			qv_freq.set(index, qv_freq.get(index) + 1);
		}

		double sumforthisword=0.0;
		double sumforthisword1=0.0;
		double index1=0.0;

		double qsumforthisword=0.0;
		double qsumforthisword1=0.0;
		double qindex1=0.0;	
		int qindex=0;
		// NORMALIZE DB


		for (int kk = 0; kk < qv.size(); kk++) {
			index = all.indexOf(db.get(kk));

			sumforthisword1 += 
					(((Math.log(db_freq.get(index))+1.0)/Math.log(2))*
							(Math.log(_indexer.totalTermFrequency()/db_freq.get(index))/Math.log(2)))*
							(((Math.log(db_freq.get(index))+1.0)/Math.log(2))*
									(Math.log(_indexer.totalTermFrequency()/db_freq.get(index))/Math.log(2)));


		}
		sumforthisword1 = Math.sqrt(sumforthisword1);


		for (int di = 0; di < qv.size(); di++) {
			index = all.indexOf(db.get(di));
			sumforthisword = 
					((Math.log(db_freq.get(index))+1.0)/Math.log(2))*
					(Math.log(_indexer.totalTermFrequency()/db_freq.get(index))/Math.log(2));

			db_freq.set(index, sumforthisword/sumforthisword1);	
		}

		/////////////////////////
		// NORMALIZE QV

		for (int kk = 0; kk < qv.size(); kk++) {
			qindex = all.indexOf(qv.get(kk));
			qsumforthisword1 += 
					(((Math.log(qv_freq.get(qindex))+1.0)/Math.log(2))*
							(Math.log(_indexer._totalTermFrequency/qv_freq.get(qindex))/Math.log(2)))*
							(((Math.log(qv_freq.get(qindex))+1.0)/Math.log(2))*
									(Math.log(_indexer.totalTermFrequency()/qv_freq.get(qindex))/Math.log(2)));

		}
		qsumforthisword1 = Math.sqrt(qsumforthisword1);

		for (int di = 0; di < qv.size(); di++) {
			qindex = all.indexOf(qv.get(di));
			qsumforthisword = 
					((Math.log(qv_freq.get(qindex))+1.0)/Math.log(2))*
					(Math.log(_indexer.totalTermFrequency()/qv_freq.get(qindex))/Math.log(2));

			qv_freq.set(qindex, qsumforthisword/qsumforthisword1);	
		}

		////////

		//printCosine(all,db_freq,qv_freq);
		score = calculateCosine(db_freq, qv_freq);

		return new ScoredDocument(doc, score);
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
  
  private double calculateCosine(Vector<Double> db_freq,
			Vector<Double> qv_freq) {
		// Calculates the cosine similarity ////////////////////////////////
		double cosNum = 0.0;
		double cosDen1 = 0.0;
		double cosDen2 = 0.0;
		double score = 0.0;

		for (int i = 0; i < db_freq.size(); i++) {
			//	System.out.println(db_freq.get(i));
			cosNum += db_freq.get(i) * qv_freq.get(i);
			cosDen1 += db_freq.get(i) * db_freq.get(i);
			cosDen2 += qv_freq.get(i) * qv_freq.get(i);
		}
		//System.out.println("");
		//System.out.println("");
		score = cosNum / (Math.sqrt(cosDen1) * Math.sqrt(cosDen2));
		//System.out.println("The Cosine Similarity is: " + score);
		// /////////////////////////////////////////////////////////////////
		return score;
	}

}
