package edu.nyu.cs.cs2580;

import java.util.Arrays;
import java.util.Scanner;
import java.util.Vector;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

	private boolean processed=false;
  public QueryPhrase(String query) {
    super(query);
  }

  @Override
  public void processQuery() {
	  if (_query == null) {
		  return;
	  }
	  if(processed){
		  return;
	  }
	  Scanner s = new Scanner(_query);
	  while (s.hasNext()) {
		  String term=s.next();
		  if(term.charAt(0)=='"'){
			  boolean ifend=false;
			  String phrase="";
			  phrase+=term;
			  while(s.hasNext()){
				  String t=s.next();
				  phrase=phrase+" "+t;
				  if(t.charAt(t.length()-1)=='"'){
					  ifend=true;
					  break;
				  }
			  }
			  phrase=phrase.replaceAll("\"", "");
			  if(ifend){
				  _tokens.add(phrase);
			  }else{
				  _tokens.addAll(Arrays.asList(phrase.split(" ")));
			  }
		  }else{
			  _tokens.add(term);
		  }
	  }	
	  s.close();
	  processed=true;
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
