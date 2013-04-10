package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

public class Bhattacharyya {
	Vector<String> queries=new Vector<String>();
	//query term prob
	HashMap<String,HashMap<String,Double>> extendedQ=new HashMap<String,HashMap<String,Double>>();
	
	public void readProbabilities(String path){
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(path));
			String line;
			while((line=reader.readLine())!=null){
				String query=line.split(":")[0];
				String terms=line.split(":")[1];
				if(extendedQ.containsKey(query)){
					HashMap<String,Double> hs=extendedQ.get(query);	
					hs.put(terms.split("\t")[0], Double.parseDouble(terms.split("\t")[1]));
					extendedQ.put(query, hs);	
				}
				else{
					queries.add(query);
					HashMap<String,Double> hs=new HashMap<String,Double>();
					hs.put(terms.split("\t")[0], Double.parseDouble(terms.split("\t")[1]));
					extendedQ.put(query, hs);		
				}	
			}
			reader.close();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public void Compute(String path){
		Vector<Double> results=new Vector<Double>();
		for(int i=0;i<queries.size()-1;i++){
			HashMap<String,Double> h1=extendedQ.get(queries.get(i));
			for(int j=i+1;j<queries.size();j++){
				HashMap<String,Double> h2=extendedQ.get(queries.get(j));
				Set<String> term=h1.keySet();
				double re=0.0;
				for(String s:term){
					if(h2.containsKey(s)){
						re+=h1.get(s)*h2.get(s);
					}
					results.add(re);
				}
			}
		}
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(path));
			int k=0;
			for(int i=0;i<queries.size()-1;i++){
				for(int j=i+1;j<queries.size();j++){
					String s=queries.get(i)+"\t"+queries.get(j)+"\t"+results.get(k)+"\n";
					writer.write(s);
					k++;
					}
				}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException{
		//while(true){
			if(args.length<2){
				System.out.println("There should be 2 parameters");
			}
			else if(args.length>2){
				System.out.println("Too many parameters.");
			}
			else{
				Bhattacharyya b=new Bhattacharyya();
				b.readProbabilities(args[0]);
				b.Compute(args[1]);
				//break;
			}
		//}
	}
}
