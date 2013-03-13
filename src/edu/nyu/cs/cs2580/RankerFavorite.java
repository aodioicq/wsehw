package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Comparator;
import java.util.Queue;
import java.util.Vector;
import java.util.PriorityQueue;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2 based on a refactoring of your favorite
 * Ranker (except RankerPhrase) from HW1. The new Ranker should no longer rely
 * on the instructors' {@link IndexerFullScan}, instead it should use one of
 * your more efficient implementations.
 */
public class RankerFavorite extends Ranker {

  public RankerFavorite(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
	  Comparator<ScoredDocument> OrderIsdn =  new Comparator<ScoredDocument>(){
			public int compare(ScoredDocument o1, ScoredDocument o2) {
				double score1 = o1.getScore();
				double score2 = o2.getScore();
				if(score2 > score1){
					return 1;
				}
				else if(score2<score1){
					return -1;
				}
				else{
					return 0;
				}
			}			
		};
	  Queue<ScoredDocument> D =  new PriorityQueue<ScoredDocument>(numResults,OrderIsdn);  
	  Vector<ScoredDocument> results = new Vector<ScoredDocument>();
	  DocumentIndexed d=(DocumentIndexed) _indexer.nextDoc(query, -1);
	  while(d._docid!=Integer.MAX_VALUE){
		  ScoredDocument s=scoreDocument(query, d);
		  D.add(s);
		  d=(DocumentIndexed) _indexer.nextDoc(query, d._docid);
	  }
	  results.addAll(D);
	  return results;
  }
  
  // refactored from cosine
  private ScoredDocument scoreDocument(Query query, DocumentIndexed di) {

	  	query.processQuery();
	  	Vector<String> qv=query._tokens;
	  	double lambda=0.5;
	  	double score=0.0;
	  	for(int i = 0; i<qv.size(); i++){
	    	String currentTerm = qv.get(i);
	    	DocumentIndexed d=(DocumentIndexed) _indexer.nextDoc(query, -1);
	    	double documentLikelihood = di.documentTermFrequency.get(i) / di.bodySize;
	    	double globalLikelihood = (double)_indexer.corpusTermFrequency(currentTerm) / (double)_indexer._totalTermFrequency;

//	    	System.out.println("INFO: "+d.getLocalTermFrequency(currentTerm) + " "+d.getTotalTerms()+" "+Document.termFrequency(currentTerm) + " "+Document.termFrequency());

	    	score *= Math.log((1-lambda)*documentLikelihood + lambda*globalLikelihood);
	    }


//	    System.out.println(did + " "+score);
		return new ScoredDocument(di,score);
	}
  

}
