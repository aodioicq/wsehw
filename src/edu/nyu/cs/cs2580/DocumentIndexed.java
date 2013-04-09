package edu.nyu.cs.cs2580;

import java.util.Vector;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
  private static final long serialVersionUID = 9184892508124423115L;
  public int bodySize;//Document body size
  public Vector<Integer> documentTermFrequency;//the term frequency of all terms in query, keep the order
  

  public DocumentIndexed(int docid) {
    super(docid);
    //documentTermFrequency=new Vector<Integer>();
  }

  public void setDocumentTermFrequency(Vector<Integer> freqs){
	  this.documentTermFrequency=new Vector<Integer>(freqs);
  }
}
