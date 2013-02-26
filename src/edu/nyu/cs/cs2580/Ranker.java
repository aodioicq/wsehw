package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.Scanner;

class Ranker {

  private Index _index;

  public Ranker(String index_source){
    _index = new Index(index_source);
  }
  public Vector<ScoredDocument> runquery(String query, int rankertype) throws Exception{
	  switch (rankertype){
	  	case 1:
	  		return runquery1(query);
	  	case 2:
	  		return runquery2(query);
	  	case 3:
	  		return runquery3(query);
	  	case 4:
	  		return runquery4(query);
	  	case 5:
	  		return runquery5(query);
	  	default:
	  		throw new Exception("Invalid ranker signal");
	  }
		  
  }
  public Vector<ScoredDocument> runquery1(String query) {
		Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
		for (int i = 0; i < _index.numDocs(); ++i) {
				retrieval_results.add(cosineSimilarity(query, i));
		}
		sortScoredDocuments(retrieval_results);
		return retrieval_results;
	}
	public Vector<ScoredDocument> runquery2(String query) {
		Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
		for (int i = 0; i < _index.numDocs(); ++i) {
				retrieval_results.add(queryLikelihood(query, i));
		}
		sortScoredDocuments(retrieval_results);
		return retrieval_results;
	}
	public Vector<ScoredDocument> runquery3(String query) {
		Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
		for (int i = 0; i < _index.numDocs(); ++i) {
				retrieval_results.add(phraseRanker(query, i));
		}
		sortScoredDocuments(retrieval_results);
		return retrieval_results;
	}
	public Vector<ScoredDocument> runquery4(String query) {
		Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
		for (int i = 0; i < _index.numDocs(); ++i) {
				retrieval_results.add(numViews(query, i));
		}
		sortScoredDocuments(retrieval_results);
		return retrieval_results;
	}
	public Vector<ScoredDocument> runquery5(String query) {
		Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
		for (int i = 0; i < _index.numDocs(); ++i) {
				retrieval_results.add(simpleLinearModel(query, i));
		}
		sortScoredDocuments(retrieval_results);
		return retrieval_results;
	}


	public ScoredDocument cosineSimilarity(String query, int did) {

		// Build query vector
		Vector<String> qv = queryVector(query);

		// Get the document vector. For hw1, you don't have to worry about the
		// details of how index works.
		Document d = _index.getDoc(did);
		Vector<String> dv = d.get_title_vector();

		Vector<String> db = d.get_body_vector();
		int num = d.get_numviews();

		// Stores all text in the document body and maps the frequency to it
		Vector<String> all = new Vector<String>();
		Vector<Integer> db_freq = new Vector<Integer>();
		Vector<Integer> qv_freq = new Vector<Integer>();
		double score;
		String key = "";
		int index = 0;

		// Adds all elements in the document body and the query into one vector
		// need to do this for qv and db
		all = populateAll(db, all);
		all = populateAll(qv, all);
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
		// /////////////////////////////////////////////////////////////////
		printCosine(all, db_freq, qv_freq);

		score = calculateCosine(db_freq, qv_freq);

		return new ScoredDocument(did, d.get_title_string(), score);
	}

	public ScoredDocument queryLikelihood(String query, int did) {

		// Build query vector
		Vector<String> qv = queryVector(query);

		// Get the document vector. For hw1, you don't have to worry about the
		// details of how index works.
		Document d = _index.getDoc(did);
		Vector<String> dv = d.get_title_vector();

		Vector<String> db = d.get_body_vector();
		int num = d.get_numviews();

		// Stores all text in the document body and maps the frequency to it
		int db_size = db.size();
		int col_size = d.termFrequency();
		Vector<Double> total_freq = new Vector<Double>();
		Vector<Double> dbXqv_freq = new Vector<Double>();
		Vector<Integer> db_freq = new Vector<Integer>();

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
			total_freq.add((double) d.termFrequency(qv.get(j)) / col_size);
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

		return new ScoredDocument(did, d.get_title_string(), score);
	}

	private Vector<String> queryVector(String query){
		Scanner s = new Scanner(query);
		Vector<String> qv = new Vector<String>();
		while (s.hasNext()) {
			String term = s.next();
			qv.add(term);
		}
		s.close();
		return qv;
	}
	
	private double calculateCosine(Vector<Integer> db_freq,
			Vector<Integer> qv_freq) {
		// Calculates the cosine similarity ////////////////////////////////
		double cosNum = 0.0;
		double cosDen1 = 0.0;
		double cosDen2 = 0.0;
		double score = 0.0;
		for (int i = 0; i < db_freq.size(); i++) {
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

	private Vector<Integer> initializeVector(int size) {
		Vector<Integer> db_freq = new Vector<Integer>();
		for (int z = 0; z < size; z++) {
			db_freq.add(0);
		}
		return db_freq;

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

	private void printCosine(Vector<String> all, Vector<Integer> db_freq,
			Vector<Integer> qv_freq) {

		for (int z = 0; z < all.size(); z++) {
			//System.out.print("|");
			//System.out.format("%5s", all.get(z));
		}
		//System.out.print("|");
		//System.out.println("");
		for (int z = 0; z < all.size(); z++) {
			//System.out.print("|");
			//System.out.format("%5d", db_freq.get(z));
		}
		//System.out.print("|");
		//System.out.println("");
		for (int z = 0; z < all.size(); z++) {
			//System.out.print("|");
			//System.out.format("%5d", qv_freq.get(z));
		}
		//System.out.print("|");

	}

	public ScoredDocument numViews(String query, int did) {
		Document d = _index.getDoc(did);
    	int numviews=d.get_numviews();
    	return new ScoredDocument(did, d.get_title_string(), numviews);
	}
	
	public ScoredDocument phraseRanker(String query, int did){
		int matches=0;
		Document d = _index.getDoc(did);
		Vector<String> qv = queryVector(query);
		Vector<String> db = d.get_body_vector();
		if(qv.size()==1){
			String q=qv.get(0);
			for(int j=0;j<db.size();++j){
				if(db.get(j).equals(q)){
					matches++;
				}
			}
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
		return new ScoredDocument(did, d.get_title_string(), matches);
	}
	
	public ScoredDocument simpleLinearModel(String query, int did){
		Document d = _index.getDoc(did);
		double cosine=cosineSimilarity(query, did)._score;
		double ql=queryLikelihood(query, did)._score;
		double phrase=phraseRanker(query, did)._score;
		double numviews=numViews(query, did)._score;
		double score=0.5*cosine+0.45*ql+0.04995*phrase+0.000005*numviews;
		return new ScoredDocument(did, d.get_title_string(), score);
	}

	private void sortScoredDocuments(Vector<ScoredDocument> retrieval_results) {
		Collections.sort(retrieval_results,new ScoredDocumentSort());
	}
	
	public void generateOutput(String queryfile) throws Exception{
		Vector<String> queries = new Vector<String>();
		try {
	      BufferedReader reader = new BufferedReader(new FileReader(queryfile));
	      try {
	        String line = null;
	        while ((line = reader.readLine()) != null){
	          queries.add(line);
	        }
	      } finally {
	        reader.close();
	      }
	    }catch (IOException ioe){
	      System.err.println("Oops " + ioe.getMessage());
	    }
		String file="";
		for(int i=1;i<=5;++i){
			switch(i){
			case 1: file="results\\hw1.1-vsm.tsv";break;
			case 2: file="results\\hw1.1-ql.tsv";break;
			case 3: file="results\\hw1.1-phrase.tsv";break;
			case 4: file="results\\hw1.1-numviews.tsv";break;
			case 5: file="results\\hw1.2-linear.tsv";break;
			}
			System.out.println("Ranking and writing into file "+file.substring(8));
			for(String query:queries){
				Vector<ScoredDocument> results=runquery(query, i);
				try {
				      BufferedWriter writer = new BufferedWriter(new FileWriter(file,true));
				      try {
				        for(ScoredDocument sd:results){
				        	writer.write(query+"\t"+sd._did+"\t"+sd._title+"\t"+sd._score);
				        	writer.newLine();
				        	writer.flush();
				        }
				      } finally {
				        writer.close();
				      }
				    }catch (IOException ioe){
				      System.err.println("Oops " + ioe.getMessage());
				    }
			}
		}
		System.out.println("Done writing into files. Check directory results for output files.");
	}

	//sort ScoredDocuments in decreasing order
	class ScoredDocumentSort implements Comparator<Object>{
	    public int compare(Object obj1, Object obj2){
	    	ScoredDocument o1=(ScoredDocument) obj1;
	    	ScoredDocument o2=(ScoredDocument) obj2;
	    	Double s1=new Double(o1._score);
	    	Double s2=new Double(o2._score);
	    	return s2.compareTo(s1);
	    }
	}
}
