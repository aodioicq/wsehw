package edu.nyu.cs.cs2580;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;


public class PseudoRelevanceFeedback {
	public static void QueryRepresentations(Indexer indexer,Vector<ScoredDocument> scoredDocs,int termNum,StringBuffer response) throws FileNotFoundException{
		String folder=indexer._options._corpusPrefix+"/";
		Vector<Integer> docs=new Vector<Integer>();
		HashMap<String, Integer> termfreqs=new HashMap<String, Integer>();
		int totalbodysize=0;
		double normal = 0.0;
		for(ScoredDocument sd:scoredDocs){
			String[] info=sd.asTextResult().split("\t");
			docs.add(Integer.parseInt(info[0]));
		}
		for(Integer id:docs){
			totalbodysize+=((DocumentIndexed)indexer.getDoc(id)).bodySize;
			String file=indexer.getDoc(id).getTitle();
			//BufferedReader reader =new BufferedReader(new FileReader(folder+file));
			String content=IndexerInvertedCompressed.getContent(folder+file);
			Scanner s = new Scanner(content); 
			while(s.hasNext()){
				String word=s.next();
				if(word==""||word==" "||word.length()==0){
        			continue;
        		}
				if(termfreqs.containsKey(word)){
					int freq=termfreqs.get(word);
					termfreqs.put(word, freq+1);
				}else{
					termfreqs.put(word, 1);
				}
			}
			s.close();
		}
		ArrayList<String> sortedTerms=sortMapByValue(termfreqs);
		if(sortedTerms.size()<termNum){
			termNum=sortedTerms.size();
		}
		for(int i=0;i<termNum;++i){
			double f=(double)termfreqs.get(sortedTerms.get(i));
			double freq =  f/ (double)totalbodysize;
			//System.out.println(f+"||"+totalbodysize);
			normal = normal + freq;
		}
		for(int i=0;i<termNum;++i){
			String term = sortedTerms.get(i);
			double freq = (double)termfreqs.get(sortedTerms.get(i)) / (double)totalbodysize;
			System.out.println(term+"\t"+freq/normal);
			response.append(term+"\t"+freq/normal+"\n");
		}
	}
	
	public static ArrayList<String> sortMapByValue(HashMap<String,Integer> map){
		int size = map.size();
		ArrayList<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(size);
		list.addAll(map.entrySet());
		ValueComparator vc = new ValueComparator();
		Collections.sort(list, vc);
		final ArrayList<String> keys = new ArrayList<String>(size);
		for (int i = 0; i < size; i++) {
		    keys.add(i, list.get(i).getKey());
		}
		return keys;	
	}
	private static class ValueComparator implements Comparator<Map.Entry<String, Integer>>{
		public int compare(Map.Entry<String, Integer> mp1, Map.Entry<String, Integer> mp2){
	        return mp2.getValue() - mp1.getValue();
	    }
	}
}
