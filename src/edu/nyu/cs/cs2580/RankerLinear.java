package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

public class RankerLinear extends Ranker {

	private ArrayList<Ranker> rankers;
	public RankerLinear(Options options,
		      CgiArguments arguments, Indexer indexer,ArrayList<Ranker> rankers) {
		    super(options, arguments, indexer);
		    this.rankers=rankers;
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
	
	private ScoredDocument scoreDocument(Query query, int did) {
		Document d = _indexer.getDoc(did);
		double cosine=((RankerCosine)rankers.get(0)).scoreDocument(query, did).getScore();
		double ql=((RankerQL)rankers.get(0)).scoreDocument(query, did).getScore();
		double phrase=((RankerPhrase)rankers.get(0)).scoreDocument(query, did).getScore();
		double numviews=numViews(did).getScore();
		double score=0.5*cosine+0.45*ql+0.04995*phrase+0.000005*numviews;
		return new ScoredDocument(d, score);
	}
	
	private ScoredDocument numViews(int did) {
		Document d = _indexer.getDoc(did);
		int numviews=d.getNumViews();
		return new ScoredDocument(d, numviews);
	}

}
