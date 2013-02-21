package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
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
		String p = "/home/user/workspace/Homework1/src/qrels.tsv";// args[0];
		eval("bing", p);
		// first read the relevance judgments into the HashMap
		// readRelevanceJudgments(p,relevance_judgments);
		// now evaluate the results from stdin
		// evaluateStdin(relevance_judgments);
	}

	public static double eval(String user_query, String p) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(p));
			try {
				String line = null;
				Vector<Double> relavence = new Vector<Double>();
				Vector<Double> prec = new Vector<Double>();
				Vector<Double> reca = new Vector<Double>();
				Vector<Double> f = new Vector<Double>();
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
					if (query.equals(user_query)) {
						relavence.add(rel);
					}

				}
				prec.add(evalPrecision(relavence, 1.0));
				prec.add(evalPrecision(relavence, 5.0));
				prec.add(evalPrecision(relavence, 10.0));
				reca.add(evalRecall(relavence, 1.0));
				reca.add(evalRecall(relavence, 5.0));
				reca.add(evalRecall(relavence, 10.0));
				for (int i = 0; i < prec.size(); i++) {
					f.add(evalF(prec.get(i), reca.get(i)));
					System.out.println("F-measure: " + f.get(i));

				}
			} finally {
				reader.close();
			}
		} catch (IOException ioe) {
			System.err.println("Oops " + ioe.getMessage());
		}
		return 0;
	}

	public static double evalPrecision(Vector<Double> relavence, double type) {
		double score = 0.0;
		// Checks for the case where there may be less documents than the
		// precision type
		for (int i = relavence.size(); i <= type; i++) {
			relavence.add(0.0);
		}
		for (int i = 0; i < type; i++) {
			score += relavence.get(i);
		}
		System.out.println("Evaluation for Precision @ " + type + " is "
				+ score / type);
		return score / type;
	}

	public static double evalRecall(Vector<Double> relavence, double type) {
		double score = 0.0;
		double totalRel = 0.0;
		// Checks for the case where there may be less documents than the
		// precision type
		for (int i = relavence.size(); i <= type; i++) {
			relavence.add(0.0);
		}
		for (int j = 0; j < relavence.size(); j++) {
			totalRel += relavence.get(j);
		}
		for (int k = 0; k < type; k++) {
			score += relavence.get(k);
		}
		if (totalRel == 0) {
			System.out.println("Evaluation for Recall @ " + type + " is 0.0");
			return 0.0;
		} else {
			System.out.println("Evaluation for Recall @ " + type + " is "
					+ score / totalRel);
			return score / totalRel;
		}
	}

	public static double evalF(Double precision, Double recall) {
		return 1 / (.5 * (1 / precision) + (1 - .5) * (1 / recall));
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

import java.util.Vector;
import java.util.HashMap;
import java.util.Scanner;

class Evaluator {

  public static void main(String[] args) throws IOException {
    HashMap < String , HashMap < Integer , Double > > relevance_judgments =
      new HashMap < String , HashMap < Integer , Double > >();
    if (args.length < 1){
      System.out.println("need to provide relevance_judgments");
      return;
    }
    String p = args[0];
    // first read the relevance judgments into the HashMap
    readRelevanceJudgments(p,relevance_judgments);
    // now evaluate the results from stdin
    evaluateStdin(relevance_judgments);
  }

  public static void readRelevanceJudgments(
    String p,HashMap < String , HashMap < Integer , Double > > relevance_judgments){
    try {
      BufferedReader reader = new BufferedReader(new FileReader(p));
      try {
        String line = null;
        while ((line = reader.readLine()) != null){
          // parse the query,did,relevance line
          Scanner s = new Scanner(line).useDelimiter("\t");
          String query = s.next();
          int did = Integer.parseInt(s.next());
          String grade = s.next();
          double rel = 0.0;
          // convert to binary relevance
          if ((grade.equals("Perfect")) ||
            (grade.equals("Excellent")) ||
            (grade.equals("Good"))){
            rel = 1.0;
          }
          if (relevance_judgments.containsKey(query) == false){
            HashMap < Integer , Double > qr = new HashMap < Integer , Double >();
            relevance_judgments.put(query,qr);
          }
          HashMap < Integer , Double > qr = relevance_judgments.get(query);
          qr.put(did,rel);
        }
      } finally {
        reader.close();
      }
    } catch (IOException ioe){
      System.err.println("Oops " + ioe.getMessage());
    }
  }

  public static void evaluateStdin(
    HashMap < String , HashMap < Integer , Double > > relevance_judgments){
    // only consider one query per call    
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      
      String line = null;
      double RR = 0.0;
      double N = 0.0;
      while ((line = reader.readLine()) != null){
        Scanner s = new Scanner(line).useDelimiter("\t");
        String query = s.next();
        int did = Integer.parseInt(s.next());
      	String title = s.next();
      	double rel = Double.parseDouble(s.next());
      	if (relevance_judgments.containsKey(query) == false){
      	  throw new IOException("query not found");
      	}
      	HashMap < Integer , Double > qr = relevance_judgments.get(query);
      	if (qr.containsKey(did) != false){
      	  RR += qr.get(did);					
      	}
      	++N;
      }
      System.out.println(Double.toString(RR/N));
    } catch (Exception e){
      System.err.println("Error:" + e.getMessage());
    }
  }
}
