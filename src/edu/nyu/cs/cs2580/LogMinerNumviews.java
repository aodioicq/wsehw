package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class LogMinerNumviews extends LogMiner {
  private HashMap<Integer, Integer> numViewMap = new HashMap<Integer, Integer>();
  private HashMap<String, Integer> postingMap = new HashMap<String, Integer>();

  public LogMinerNumviews(Options options) {
    super(options);
  }

  /**
   * This function processes the logs within the log directory as specified by
   * the {@link _options}. The logs are obtained from Wikipedia dumps and have
   * the following format per line: [language]<space>[article]<space>[#views].
   * Those view information are to be extracted for documents in our corpus and
   * stored somewhere to be used during indexing.
   *
   * Note that the log contains view information for all articles in Wikipedia
   * and it is necessary to locate the information about articles within our
   * corpus.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException {
    System.out.println("Computing using " + this.getClass().getName());
    	readPosting();
      	String line;
		String[] parseLine;
		File log = new File("data/log/20130301-160000.log");
		BufferedReader br = new BufferedReader(new FileReader(
				log.getAbsoluteFile()));

		while ((line = br.readLine()) != null) {
			parseLine = line.split(" ");

			if (postingMap.containsKey(parseLine[1])) {
				numViewMap.put(postingMap.get(parseLine[1]),
						Integer.parseInt(parseLine[2]));
			}
		}
		br.close();
		write();
    return;
  }
  private void readPosting() throws IOException {
	 		String line;
			String[] parseLine;
			File post = new File("data/postingmap.txt");
			BufferedReader br = new BufferedReader(new FileReader(
					post.getAbsoluteFile()));

			while ((line = br.readLine()) != null) {
				parseLine = line.split("\t");
				postingMap.put(parseLine[0],Integer.parseInt(parseLine[1]));		
			}
			br.close();
  }
  private void write() throws IOException{
	
	    
      File numview = new File("data/numview.txt");
        if (!numview.exists()) {
			numview.createNewFile();
		}
        String newline = System.getProperty("line.separator");
		String out = " ";
		TreeMap<Integer, Integer> tm = new TreeMap<Integer, Integer>(numViewMap);

		BufferedWriter bw = new BufferedWriter(new FileWriter(
				numview.getAbsoluteFile()));

		for (Entry<Integer, Integer> entry : tm.entrySet()) {
			out = entry.getKey().toString() + "\t"
					+ entry.getValue().toString();
			bw.write(out);
			bw.write(newline);
		}
		bw.close();
	}
   
  
  /**
   * During indexing mode, this function loads the NumViews values computed
   * during mining mode to be used by the indexer.
   * 
   * @throws IOException
   */
  @Override
  public Object load() throws IOException {
	  System.out.println("Loading using " + this.getClass().getName());
		String numViews = "data/numview.txt";
			String line;
			String[] parsed;
			File nv = new File(numViews);
			BufferedReader br = new BufferedReader(new FileReader(
					nv.getAbsoluteFile()));
			while ((line = br.readLine()) != null) {
				parsed = line.split("\t");
				numViewMap.put(Integer.parseInt(parsed[0]), Integer.parseInt(parsed[1]));
			}
			br.close();
	  return null;
  }
  

		
}
