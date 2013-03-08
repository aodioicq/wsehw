package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

public class RankerPhrase extends Ranker {

	public RankerPhrase(Options options,
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
		int matches=0;
		Document d = _indexer.getDoc(did);
		query.processQuery();
		Vector<String> qv=query._tokens;
		Vector<String> db = ((DocumentFull) d).getConvertedBodyTokens();  
		if(qv.size()==1 || db.size()==1){
			matches=0;
		}else{
			for(int i=0;i<qv.size()-1;++i){
				String bigram=qv.get(i)+" "+qv.get(i+1);
				for(int j=0;j<db.size()-1;++j){
					String docbigram=db.get(j)+" "+db.get(j+1);
					if(bigram.equals(docbigram)){
						matches++;
					}
				}
			}
		}
		return new ScoredDocument(d, matches);
	}

}
