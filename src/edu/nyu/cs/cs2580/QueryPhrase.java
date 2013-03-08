package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  public QueryPhrase(String query) {
    super(query);
  }

  @Override
  public void processQuery() {
	  if (_query == null) {
		  return;
	  }
	  Scanner s = new Scanner(_query);
	  while (s.hasNext()) {
		  String term=s.next();
		  if(term.charAt(0)=='"'){
			  String phrase="";
			  phrase+=term;
			  while(s.hasNext()){
				  String t=s.next();
				  phrase=phrase+" "+t;
				  if(t.charAt(t.length()-1)=='"'){
					  break;
				  }
			  }
			  phrase=phrase.replaceAll("\"", "");
			  _tokens.add(phrase);
		  }else{
			  _tokens.add(term);
		  }
	  }	
	  s.close();
  }
  /*
  public static void main(String[] args) {
	  String a="\"new york\" city \"living condition\"";
	  System.out.println("original:"+a);
	  QueryPhrase qp=new QueryPhrase(a);
	  qp.processQuery();
	  //System.out.println(qp._query);
	  for(String s:qp._tokens){
		  System.out.println(s);
	  }
  }
  */
}
