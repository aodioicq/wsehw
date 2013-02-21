package edu.nyu.cs.cs2580;

import java.util.Vector;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Ranker ranker = new Ranker("corpus.tsv");
		Vector < ScoredDocument > result =ranker.runquery("", 3);
		for(int i=0;i<result.size();++i){
			System.out.print(result.get(i)._title+": ");
			System.out.println(result.get(i)._score);
		}
		
	}

}
