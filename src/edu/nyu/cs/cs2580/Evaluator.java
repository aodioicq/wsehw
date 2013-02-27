package edu.nyu.cs.cs2580;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Collections;
import java.util.Vector;
import java.util.HashMap;
import java.util.Scanner;

class Evaluator {

	public static void main(String[] args) throws IOException {
		HashMap<String, HashMap<Integer, Double>> relevance_judgments = new HashMap<String, HashMap<Integer, Double>>();
		// if (args.length < 1){
		// System.out.println("need to provide relevance_judgments");
		// return;
		// }
		String p = "data/qrels.tsv";// args[0];
		eval("bing", p, 1);
		// first read the relevance judgments into the HashMap
		// readRelevanceJudgments(p,relevance_judgments);
		// now evaluate the results from stdin
		// evaluateStdin(relevance_judgments);
	}
	public static Vector<String[]> retrieveRank(int type)
	{
		Vector<String[]> rankData = new Vector<String[]>();
		BufferedReader br = null;
		String fileName = "";
		switch (type){
		case 1:	fileName = "results\\hw1.1-vsm.tsv";
		break;
		case 2: fileName = "results\\hw1.1-ql.tsv";
		break;
		case 4:	fileName = "results\\hw1.1-numviews.tsv";
		break;
		case 5: fileName = "results\\hw1.1-ql.tsv";
		break;
		case 3:	fileName = "results\\hw1.2-linear.tsv";
		break;

		}
		String sCurrentLine;

		try {
			br = new BufferedReader(new FileReader(fileName));
			while ((sCurrentLine = br.readLine()) != null) {
				// | 0 | query | 0 | | 1 | sd._did | 1 | | 2 | sd._title | 2 | | 3 | sd._score | 3 |
				rankData.add(sCurrentLine.split("\t"));
				//System.out.println(sCurrentLine);
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return rankData;

	}
	public static Vector<Double> docNDCG(Vector<String> relQuery,Vector<Integer>relDiD,Vector<String> relGrade,
			Vector<String> docQuery,Vector<Integer> docDiD,String user_query)
	{
		Vector<Double> NDCG = new Vector<Double>();
		int docIndex = 0;
		int relIndex = 0;
		String grade = null;
		for(int i = 0;i<docQuery.size();i++)
		{
			if(user_query.equals(docQuery.get(i)))
			{
				docIndex = docDiD.get(i);
				
				if(relDiD.contains(docIndex))
				{
				relIndex = relDiD.indexOf(docIndex);
				if(user_query.equals(relQuery.get(relIndex)))
				{
					grade = relGrade.get(relIndex); // gets the grade of the first doc id
				}
				else
				{
					grade = "Bad";
				}}
				else 
				{
					grade = "Bad";
				}
					switch(grade)
					{
					case "Perfect": NDCG.add(10.0); 
					break;
					case "Excellent": NDCG.add(7.0);
					break;
					case "Good": NDCG.add(5.0);
					break;
					case "Fair": NDCG.add(1.0);
					break;
					case "Bad": NDCG.add(0.0);
					}
				}
		}
		return NDCG;
	}
	public static Vector<Double> docRelavence(Vector<String> relQuery,Vector<Integer>relDiD,Vector<String> relGrade,
			Vector<String> docQuery,Vector<Integer> docDiD,String user_query) // probably do not need user_query since the doc stuff is already ranked based on the query
			{
		Vector<Double> relavence = new Vector<Double>();
		double rel = 0.0;
		int docIndex = 0;
		int relIndex = 0;
		String grade = null;
		
		for(int i = 0;i<docQuery.size();i++)
		{
			if(user_query.equals(docQuery.get(i)))
			{
				docIndex = docDiD.get(i);
				if(relDiD.contains(docIndex))
				{
					
						relIndex = relDiD.indexOf(docIndex);
					
					if(user_query.equals(relQuery.get(relIndex)))
					{
						grade = relGrade.get(relIndex); // gets the grade of the first doc id
					}
					else
					{
						grade = "Bad";
					}
				}
				else 
				{
					grade = "Bad";
				}
	
				// convert to binary relevance
				if ((grade.equals("Perfect"))
						|| (grade.equals("Excellent"))
						|| (grade.equals("Good"))) {
					rel = 1.0;
				}
				else
				{
					rel = 0.0;
				}
			relavence.add(rel);
			}
		}
		
		//for(int j = 0;j<30;j++)
			//System.out.println(relavence.get(j));
		return relavence;

			}
	public static double eval(String user_query, String p,int type) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(p));
			try {

				String line = null;
				/*
				 * See about cleaning up the number of vector variables so there aren't as many (if possible)
				 */
				// | 0 | query | 0 | | 1 | sd._did | 1 | | 2 | sd._title | 2 | | 3 | sd._score | 3 |
				Vector<String[]> rankData = retrieveRank(type);

				Vector<Integer> docDiD = new Vector<Integer>();
				Vector<Double> relavence = new Vector<Double>();
				Vector<String> docQuery = new Vector<String>();
				Vector<Double> prec = new Vector<Double>();
				Vector<Double> reca = new Vector<Double>();
				Vector<Double> f = new Vector<Double>();
				double[] precATreca = new double[11];
				Vector<Double> relavenceNDCG = new Vector<Double>();
				Vector<Double> output = new Vector<Double>();
				Vector<String> relQuery = new Vector<String>();
				Vector<Integer> relDiD = new Vector<Integer>();
				Vector<String> relGrade = new Vector<String>();
				String q = null;
				for(int i = 0;i<rankData.size();i++)
				{

					docQuery.add(rankData.get(i)[0]);
					docDiD.add(Integer.parseInt(rankData.get(i)[1]));
				}
				//System.out.println("RANK DATA SIZE " + rankData.get(1047)[0]);
				while ((line = reader.readLine()) != null) {
					// parse the query,did,relevance line
					Scanner s = new Scanner(line).useDelimiter("\t");
					q = s.next();
					if(q.equals(user_query))
					{
					relQuery.add(q);
					relDiD.add(Integer.parseInt(s.next()));
					relGrade.add(s.next());
					}
					else
					{
						s.next();
						s.next();
					}
						
					s.close();
				}
				
				relavence = docRelavence(relQuery,relDiD,relGrade,docQuery,docDiD,user_query);
				relavenceNDCG = docNDCG(relQuery,relDiD,relGrade,docQuery,docDiD,user_query);
				// Precision
				prec.add(evalPrecision(relavence, 1.0));
				prec.add(evalPrecision(relavence, 5.0));
				prec.add(evalPrecision(relavence, 10.0));
				//
				// Recall
				reca.add(evalRecall(relavence, 1.0));
				reca.add(evalRecall(relavence, 5.0));
				reca.add(evalRecall(relavence, 10.0));
				// 
				// F-value
				for (int i = 0; i < prec.size(); i++) {
					f.add(evalF(prec.get(i), reca.get(i)));
					System.out.println("F-measure: " + f.get(i));

				}
				//

				// Add to output
				for (int j = 0;j<prec.size();j++){
					output.add(prec.get(j));
				}
				for (int k = 0;k<prec.size();k++){
					output.add(reca.get(k));
				}
				for (int l = 0;l<prec.size();l++){
					output.add(prec.get(l));
				}
				// precision at recall value
				precATreca = evalPreciAtRecall(relavence ,prec,reca);
					for (int m = 0;m<precATreca.length;m++)
					{
					output.add(precATreca[m]);
					}
				//
				// Average Precision
					output.add(evalAvgPrecision(relavence));
				
				// NDCG
						output.add(evalNDCG(relavenceNDCG));
				//
				// Reciprocal
						output.add(evalReciprocal(relavence));
				//

			} finally {
				reader.close();
			}
		} catch (IOException ioe) {
			System.err.println("Oops " + ioe.getMessage());
		}
		return 0;
	}

	public static double evalPrecision(Vector<Double> relavence, double k_value) {
		double score = 0.0;
		// Checks for the case where there may be less documents than the
		// precision k_value
		
		for (int i = relavence.size(); i <= k_value; i++) {
			relavence.add(0.0);
		}
		for (int i = 0; i < k_value; i++) {
			score += relavence.get(i);
		}
		System.out.println("Evaluation for Precision @ " + k_value + " is "
				+ score / k_value);
		return score / k_value;
	}

	public static double evalRecall(Vector<Double> relavence, double k_value) {
		double score = 0.0;
		double totalRel = 0.0;
		// Checks for the case where there may be less documents than the
		// precision k_value
		for (int i = relavence.size(); i <= k_value; i++) {
			relavence.add(0.0);
		}
		for (int j = 0; j < relavence.size(); j++) {
			totalRel += relavence.get(j);
		}

		for (int k = 0; k < k_value; k++) {
			score += relavence.get(k);
		}
		if (totalRel == 0) {
			System.out.println("Evaluation for Recall @ " + k_value + " is 0.0");
			return 0.0;
		} else {
			System.out.println("Evaluation for Recall @ " + k_value + " is "
					+ score / totalRel);
			return score / totalRel;
		}
	}

	public static double evalF(Double precision, Double recall) {
		return 1 / (.5 * (1 / precision) + (1 - .5) * (1 / recall));
	}
	/**
	 * @param precision
	 * @param recall
	 * @return
	 */
	public static double[] evalPreciAtRecall(Vector<Double> rel, Vector<Double> precision,Vector<Double> recall)
	{
		
		double[] precisions=new double[10];
		double[] recalls=new double[10];
		double[] precisionsAtRecall=new double[11];
		for(int i=0;i<10;++i){
			precisions[i]=evalPrecision(rel, i+1);
			recalls[i]=evalRecall(rel, i+1);
		}
		int start=0;
		for(int i=0;i<11;++i){
			double maxPrecision=0.0;
			boolean foundstart=false;
			double index=(double)i/10;
			for(int j=0;j<10;++j){
				if(recalls[j]>=index){
					start=j;
					foundstart=true;
					break;
				}
			}
			if(!foundstart){
				precisionsAtRecall[i]=0;
				continue;
			}
			for(int j=start;j<10;++j){
				if(precisions[j]>maxPrecision){
					maxPrecision=precisions[j];
				}
			}
			precisionsAtRecall[i]=maxPrecision;
		}
		for(int i=0;i<precisionsAtRecall.length;i++)
		{
			System.out.print(precisionsAtRecall[i] + " |");
		}
		return precisionsAtRecall;
		/*
		double score = 0.0;
		double[] temp = new double[11];
		int index = 0;
		// 0.0 1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0
		// 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 01.0
		for(int i = 0;i<precision.size();i++)
		{
			index = (int) (recall.get(i)*10);
			if (precision.get(i) > temp[index])
			{
				temp[index] = precision.get(i);
				for(int j = index+1;j<11;j++)
				{
					temp[j] = precision.get(i);
				}
			}
		}
		System.out.print("| ");
		for(int i=0;i<temp.length;i++)
		{
			System.out.print(temp[i] + " |");
		}
		System.out.println("");
		return temp;
		*/

	}
	public static double evalAvgPrecision(Vector<Double> relavence)
	{
		double score = 0.0;
		double AP = 0.0;
		double temp = 0.0;
		// Checks for the case where there may be less documents than the
		// precision k_value

		for (int i = 0; i < relavence.size(); i++) {

			temp = relavence.get(i);
			score += temp;

			AP += (score / (i+1));

		//	System.out.println("At " + (i+1) + " the score is " + score + " the Avg Preision is " + AP / score);
		}
		System.out.println("Evaluation for AVG Precision is " + AP / score);
		return AP / score;
	}
	public static double evalNDCG(Vector<Double> relavence)
	{
		double DCG = 0.0;
		double sortedDCG = 0.0;
		double NDCG = 0.0;
		DCG = evalDCG(relavence);
		Collections.sort(relavence);
		Collections.reverse(relavence);
		sortedDCG = evalSortedDCG(relavence);
		NDCG = DCG/sortedDCG;
		System.out.println("NDCG is " + NDCG);

		return NDCG;
	}
	public static double evalDCG(Vector<Double> relavence)
	{
		double DCG = 0.0;
		double gain = 0.0;
		for (int i = 0;i<relavence.size();i++)
		{
			gain = relavence.get(i);
			DCG += gain / (Math.log(i+2)/Math.log(2));
			//System.out.println("DCG at " + (i+1) + " is " + DCG);
		}

		return DCG;
	}
	public static double evalSortedDCG(Vector<Double> relavence)
	{
		double DCG = 0.0;
		double gain = 0.0;
		for (int i = 0;i<relavence.size();i++)
		{
			gain = relavence.get(i);
			DCG += gain / (Math.log(i+2)/Math.log(2));
			//System.out.println("sDCG at " + (i+1) + " is " + DCG);
		}

		return DCG;
	}

	public static double evalReciprocal(Vector<Double> relavence)
	{
		double score = 0.0;
		int index = 0;
		while(score < 1.0)
		{
			score += relavence.get(index);
			index++;
		}
		score = 1.0 / index;
		System.out.println("Reciprocal is " + score);
		return score;

	}
	public static void readRelevanceJudgments(String p,
			HashMap<String, HashMap<Integer, Double>> relevance_judgments) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(p));
			try {
				String line = null;
				while ((line = reader.readLine()) != null) {
					// parse the query,did,relevance line
					Scanner s = new Scanner(line).useDelimiter("\t");
					String query = s.next();
					int did = Integer.parseInt(s.next());
					String grade = s.next();
					double rel = 0.0;
					// convert to binary relevance
					if ((grade.equals("Perfect"))
							|| (grade.equals("Excellent"))
							|| (grade.equals("Good"))) {
						rel = 1.0;
					}
					if (relevance_judgments.containsKey(query) == false) {
						HashMap<Integer, Double> qr = new HashMap<Integer, Double>();
						relevance_judgments.put(query, qr);
					}
					HashMap<Integer, Double> qr = relevance_judgments
							.get(query);
					qr.put(did, rel);
				}
			} finally {
				reader.close();
			}
		} catch (IOException ioe) {
			System.err.println("Oops " + ioe.getMessage());
		}
	}

	public static void evaluateStdin(
			HashMap<String, HashMap<Integer, Double>> relevance_judgments) {
		// only consider one query per call
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					System.in));

			String line = null;
			double RR = 0.0;
			double N = 0.0;
			while ((line = reader.readLine()) != null) {
				Scanner s = new Scanner(line).useDelimiter("\t");
				String query = s.next();
				int did = Integer.parseInt(s.next());
				String title = s.next();
				double rel = Double.parseDouble(s.next());
				if (relevance_judgments.containsKey(query) == false) {
					throw new IOException("query not found");
				}
				HashMap<Integer, Double> qr = relevance_judgments.get(query);
				if (qr.containsKey(did) != false) {
					RR += qr.get(did);
				}
				++N;
			}
			System.out.println(Double.toString(RR / N));
		} catch (Exception e) {
			System.err.println("Error:" + e.getMessage());
		}
	}
}
