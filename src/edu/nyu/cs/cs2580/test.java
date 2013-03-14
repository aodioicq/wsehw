package edu.nyu.cs.cs2580;
import java.io.IOException;

import edu.nyu.cs.cs2580.SearchEngine.Options;

public class test {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
	    //Ranker ranker = new Ranker("data/corpus.tsv");
	    //ranker.runquery2("bing");
		Options op=null;
		IndexerInvertedOccurrence doc=new IndexerInvertedOccurrence(op);
		doc.constructIndex();
		//doc.loadIndex();
		
	}

}
