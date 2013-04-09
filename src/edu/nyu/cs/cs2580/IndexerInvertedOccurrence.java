package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import edu.nyu.cs.cs2580.SearchEngine.Options;

public class IndexerInvertedOccurrence extends Indexer {
	//term,did(positions)
	private HashMap<String, HashMap<Integer, Vector<Integer>>> _index;
	private Vector<DocumentIndexed> _documents;
	
	public IndexerInvertedOccurrence(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}
	
	@Override
	public Document getDoc(int docid) {
		return _documents.get(docid);
	}

	@Override
	public Document nextDoc(Query query, int docid) {
		DocumentIndexed doc = new DocumentIndexed(docid);
		String indexFile = _options._indexPrefix + "/occurrence/index.idx";
		HashMap<String, Vector<Integer>> termsids = new HashMap<String, Vector<Integer>>(); 
		HashMap<String, Vector<Integer>> termsfreqs = new HashMap<String, Vector<Integer>>(); 
		HashMap<String, Vector<Integer>> phrasetermsids = new HashMap<String, Vector<Integer>>(); 
		HashMap<String, Vector<Integer>> phrasetermsfreqs = new HashMap<String, Vector<Integer>>(); 
		HashMap<String, Vector<Vector<Integer>>> phrasetermsoccurs = new HashMap<String, Vector<Vector<Integer>>>(); 
		
		Set<String> phraseterms=new HashSet<String>();
		for(String term:query._tokens){
			if(term.contains(" ")){
				String[] terms=term.split(" ");
				phraseterms.addAll(Arrays.asList(terms));
			}
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(indexFile));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] data = line.split("\t");
				if (query._tokens.contains(data[0])) {
					String pos = data[1];
					Vector<Integer> docids = new Vector<Integer>();
					Vector<Integer> freqs = new Vector<Integer>();
					for (String s : pos.split("\\|")) {
						String[] nums=s.split(",");
						docids.add(Integer.parseInt(nums[0])); 
						freqs.add(Integer.parseInt(nums[1])); 
					}
					termsids.put(data[0], docids); // save term and its docids
					termsfreqs.put(data[0], freqs);
				}
				if(phraseterms.contains(data[0])){
					String pos = data[1];
					Vector<Integer> docids = new Vector<Integer>();
					Vector<Vector<Integer>> occurs = new Vector<Vector<Integer>>();
					for (String s : pos.split("\\|")) {
						String[] nums=s.split(",");
						docids.add(Integer.parseInt(nums[0]));
						Vector<Integer> os=new Vector<Integer>();
						for(int x=2;x<nums.length;++x){
							os.add(Integer.parseInt(nums[x]));
						}
						occurs.add(os); 
					}
					phrasetermsids.put(data[0], docids);
					phrasetermsoccurs.put(data[0], occurs);
				}
			}// end for read file
			//check for phrases
			
			reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		HashMap<String, Vector<Integer>> phrasesids = new HashMap<String, Vector<Integer>>(); 
		for(String term:query._tokens){
			if(term.contains(" ")){
				int i=0;
				String[] words=term.split(" ");
				Vector<Integer> docids=phrasetermsids.get(words[i]);
				Vector<Integer> freqs=new Vector<Integer>();
				while(i<words.length-1){
					Vector<Integer> nextdocids=phrasetermsids.get(words[i+1]);
					Vector<Integer> common=getCommon(docids, nextdocids);
					Vector<Integer> finalcommon=new Vector<Integer>();
					for(Integer c:common){
						int a=docids.indexOf(c);
						int b=nextdocids.indexOf(c);
						Vector<Integer> dococcurs=phrasetermsoccurs.get(words[i]).get(a);
						Vector<Integer> nextdococcurs=phrasetermsoccurs.get(words[i+1]).get(b);
						int freq=0,fc=-1;
						for(Integer d:dococcurs){
							int nextpos=d+1;
							if(nextdococcurs.contains(nextpos)){
								fc=c;
								if(i==words.length-2){
									freq++;
								}
							}
						}
						if(fc!=-1){
							finalcommon.add(c);
							if(i==words.length-2){
								freqs.add(freq);
							}
						}
					}
					docids.clear();
					docids.addAll(finalcommon);
					i++;
				}
				phrasesids.put(term, docids);
				phrasetermsfreqs.put(term, freqs);
			}
		}
		
		Vector<Integer> ids = new Vector<Integer>();
		int result;
		int id;
		for (String term : query._tokens) {
			if(term.contains(" ")){
				if (phrasesids.get(term) != null){
					id = next(term, docid, phrasesids.get(term));
					if(id!=-1)
						ids.add(id);
				}
			}else{
				if (termsids.get(term) != null) {
					id = next(term, docid, termsids.get(term));
					if(id!=-1)
						ids.add(id);
				}
			}
		}
		if (ids.size() != query._tokens.size()) // no doc includes all the terms
			return null;

		if (ids.size() == 1 || find(ids)) {
			result = ids.get(0);
			doc = (DocumentIndexed) getDoc(result);
			Vector<Integer> freqs = new Vector<Integer>();
			for (String term : query._tokens) {
				int index=0;
				if(term.contains(" ")){
					index=phrasesids.get(term).indexOf(result);
					freqs.add(phrasetermsfreqs.get(term).get(index));
				}else{
					index=termsids.get(term).indexOf(result);
					freqs.add(termsfreqs.get(term).get(index));
				}
			}
			doc.setDocumentTermFrequency(freqs);
			return doc;

		} else {
			return nextDoc(query, max(ids) - 1);
		}
	}
	
	private Vector<Integer> getCommon(Vector<Integer> v1, Vector<Integer> v2){
		int i=0,j=0;
		Vector<Integer> result=new Vector<Integer>();
		while(i<v1.size()&&j<v2.size()){
			int a=v1.get(i);
			int b=v2.get(j);
			if(a==b){
				result.add(a);
			}else if(a>b){
				j++;
			}else{
				i++;
			}
		}
		return result;
	}

	private boolean find(Vector<Integer> ids) {
		int first = ids.get(0);
		for (int i = 1; i < ids.size(); i++) {
			if (ids.get(i) != first)
				return false;
		}
		return true;
	}

	private int max(Vector<Integer> ids) {
		int max = 0;
		for (int i = 0; i < ids.size(); i++) {
			if (ids.get(i) > max)
				max = ids.get(i);
		}
		return max;
	}
	
	@Override
	public void constructIndex() throws IOException {
		HashMap<String, HashMap<Integer, Vector<Integer>>> _index = new HashMap<String, HashMap<Integer, Vector<Integer>>>();
		this._totalTermFrequency=0;
		this._numDocs=0;
		String corpus = _options._corpusPrefix + "/";
		System.out.println("Construct index from: " + corpus);
		int count=0;
		int did=0;
		SortedSet<String> allterms=new TreeSet<String>();
		File root = new File(corpus);
        File[] files = root.listFiles();
        String documents = _options._indexPrefix + "/occurrence/documents.idx";
		BufferedWriter out2 = new BufferedWriter(new FileWriter(documents));
        for (int i = 0; i < files.length; i++) {
        	HashMap<String, Vector<Integer>> positions = new HashMap<String, Vector<Integer>>();
        	String file=corpus +files[i].getName();
        	String content=getContent(file);
        	if(content==null){
        		continue;
        	}
        	//DocumentIndexed doc = new DocumentIndexed(did);
        	//doc.setUrl(files[i].getName());
        	//doc.setTitle(files[i].getName());
        	Scanner s = new Scanner(content); 
        	int p = 1;
        	while(s.hasNext()){
        		String word=s.next();
        		//System.out.println(word);
        		if(word==""||word==" "||word.length()==0){
        			continue;
        		}
        		++_totalTermFrequency;
        		allterms.add(word);
				if(!positions.containsKey(word)){
					Vector<Integer> postmp = new Vector<Integer>();
					positions.put(word, postmp);
				}
				Vector<Integer> pos=positions.get(word);
				pos.add(p);
				p++;
        	}
        	String filePath = _options._indexPrefix + "/occurrence/documents/" + did+".idx";
        	File docfolder = new File(_options._indexPrefix + "/occurrence/documents");
    		if (!docfolder.exists()) {
    			docfolder.mkdir();
    		}
			BufferedWriter out3 = new BufferedWriter(new FileWriter(filePath));
        	for (String term : positions.keySet()) {
    			if (!_index.containsKey(term)) {
    				HashMap<Integer, Vector<Integer>> doc_pos = new HashMap<Integer, Vector<Integer>>();
    				doc_pos.put(did, positions.get(term));
    				_index.put(term, doc_pos);
    			} else {
    				HashMap<Integer, Vector<Integer>> plist = _index.get(term);
    				plist.put(did, positions.get(term));
    				_index.put(term, plist);
    			}
    			out3.write(term + "\t" + positions.get(term).size());
				out3.newLine();
    		}
			//out3.flush();
			out3.close();
        	//doc.bodySize=p-1;
        	out2.write(did+"\t"+files[i].getName()+"\t"+files[i].getName()+"\t"+(p-1));
        	out2.newLine();
        	//out2.flush();
        	//_documents.add(doc);
        	did++;
        	if((i/200)>count||(i==files.length-1)){
        		String foldername = _options._indexPrefix+"/occurrence";
        		File tmpfolder = new File(foldername);
    			if (!tmpfolder.isDirectory()) {
    				tmpfolder.mkdir();
    			}
    			File tmpfile = new File(foldername+"/tmp"+count+".idx");
    			FileWriter fileWritter = new FileWriter(tmpfile);
    			BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
        		SortedSet<String> keys = new TreeSet<String>(_index.keySet());
        		for (String term : keys) {
        			bufferWritter.write(term+"\t");
        			StringBuilder value = new StringBuilder();
        			SortedSet<Integer> dids = new TreeSet<Integer>(_index.get(term).keySet());
        			for(Integer d:dids){
        				value.append(d);
        				value.append(",").append(_index.get(term).get(d).size());
    					for (int pos : _index.get(term).get(d)) {
    						value.append(",").append(pos);
    					}
    					value.append("|");
        			}
        			bufferWritter.write(value.toString());
        			bufferWritter.newLine();
        		}
        		bufferWritter.close();
        		_index.clear();
        		count++;
        	}
        }
        this._numDocs=did;
        String statistics = _options._indexPrefix + "/occurrence/statistics.idx";
		BufferedWriter out = new BufferedWriter(new FileWriter(statistics));
		out.write(""+_numDocs);
		out.newLine();
		out.write(""+_totalTermFrequency);
		out.flush();
		out.close();
		//String doc = _options._indexPrefix + "/occurrence/documents.idx";
		//BufferedWriter out2 = new BufferedWriter(new FileWriter(doc));
		/*
		for(DocumentIndexed doci:_documents){
			out2.write(doci._docid+"\t"+doci.getUrl()+"\t"+doci.getTitle()+"\t"+doci.bodySize);
			out2.newLine();
		}
		out2.flush();*/
		out2.close();
		merge(allterms);

	}
	
	private void merge(SortedSet<String> allterms) throws IOException{
		String foldername = _options._indexPrefix+"/occurrence";
		//SortedSet<Integer> ids=new TreeSet<Integer>();
		HashMap<Integer, String> currentLine=new HashMap<Integer, String>();
		File root = new File(foldername);
        File[] files = root.listFiles();
        BufferedReader[] readers = new BufferedReader[files.length-2];
        for (int i = 0; i < files.length-3; i++) {
        	readers[i] = new BufferedReader(new FileReader(foldername+"/tmp"+i+".idx"));
        }
        File index = new File(foldername+"/index.idx");
		if (!index.exists()) {
			index.createNewFile();
		}
		FileWriter fileWritter = new FileWriter(index);
		BufferedWriter indexWritter = new BufferedWriter(fileWritter);
		/*
		File log = new File(foldername+"/log.txt");
		if (!log.exists()) {
			log.createNewFile();
		}
		FileWriter lWritter = new FileWriter(log);
		BufferedWriter logWritter = new BufferedWriter(lWritter);*/
        Iterator it = allterms.iterator();
        while(it.hasNext()){
        	String term=(String)it.next();
        	indexWritter.write(term+"\t");
        	//logWritter.write("["+term+"]");
        	//logWritter.newLine();
        	for (int i = 0; i < files.length-3; i++) {
        		//logWritter.write("file: "+i);
        		//logWritter.newLine();
        		String line;
        		boolean stored=false;
        		if(currentLine.containsKey(i)){
        			line=currentLine.get(i);
        			stored=true;
        			//logWritter.write("get saved line: ("+line+")");
        			//logWritter.newLine();
        		}else{
        			line = readers[i].readLine();
        			//logWritter.write("read in new line: "+line);
        			//logWritter.newLine();
        		}
        		if(line != null){
        			String[] l = line.split("\t");
        			//logWritter.write("term: "+term+" l[0]: "+l[0]);
        			//logWritter.write(l[0].equals(term)?"  =":"    !=");
        			//logWritter.newLine();
        			if(l[0].equals(term)){
        				indexWritter.write(l[1]);
        				//logWritter.write("write in index: "+l[1]);
            			//logWritter.newLine();
        				if(stored){
        					currentLine.remove(i);
        					//logWritter.write("remove saved line");
        					//logWritter.newLine();
        				}
        			}else{
        				if(!stored){
        					currentLine.put(i, line);
        					//logWritter.write("save line: "+line);
        					//logWritter.newLine();
        				}
        			}
        		}
        	}
	        indexWritter.newLine();
        }
        for (int i = 0; i < files.length-3; i++) {
        	readers[i].close();
        	File file = new File(foldername+"/tmp"+i+".idx");
        	file.delete(); 
        }
        indexWritter.flush();
        indexWritter.close();
        //logWritter.flush();
        //logWritter.close();
	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		String indexFile = _options._indexPrefix + "/occurrence/statistics.idx";
		String docFile = _options._indexPrefix + "/occurrence/documents.idx";
		_documents=new Vector<DocumentIndexed>();
		System.out.println("Load index from: " + indexFile);
		BufferedReader reader = new BufferedReader(new FileReader(indexFile));
		String line = null;
		if ((line = reader.readLine()) != null)
			this._numDocs = Integer.parseInt(line);
		if ((line = reader.readLine()) != null)
			this._totalTermFrequency = Integer.parseInt(line);
		reader.close();
		reader = new BufferedReader(new FileReader(docFile));
		line = null;
		while((line = reader.readLine()) != null){
			String[] l = line.split("\t");
			DocumentIndexed doc=new DocumentIndexed(Integer.parseInt(l[0]));
			doc.setTitle(l[1]);
			doc.setUrl(l[2]);
			doc.bodySize=Integer.parseInt(l[3]);
			_documents.add(doc);			
		}
		reader.close();
	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		String indexFile = _options._indexPrefix + "/index.idx";
		if(term.contains(" ")){
			return 0;
		}else{
			try {
				BufferedReader reader = new BufferedReader(new FileReader(indexFile));
				String line;
				int termDocFreq = 0;
				out: while ((line = reader.readLine()) != null) {
					Scanner s = new Scanner(line).useDelimiter("\t");
					String t = null,docs = null;
					while (s.hasNext()) {
						t = s.next();
						docs=s.next();
					}
					if (t.equals(term)) {
						String[] data = docs.split("\\|");
						termDocFreq = data.length;
						s.close();
						break out;
					}
					s.close();
				}
				reader.close();
				return termDocFreq;
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

	@Override
	public int corpusTermFrequency(String term) {
		String indexFile = _options._indexPrefix + "/occurrence/index.idx";
		if(term.contains(" ")){
			try {
				String[] terms=term.split(" ");
				ArrayList<String> words=(ArrayList<String>) Arrays.asList(terms);
				HashMap<String,Vector<Integer>> docids=new HashMap<String,Vector<Integer>>();
				HashMap<String,Vector<Vector<Integer>>> docfreqs=new HashMap<String,Vector<Vector<Integer>>>();
				BufferedReader reader = new BufferedReader(new FileReader(indexFile));
				String line;
				while ((line = reader.readLine()) != null) {
					Scanner s = new Scanner(line).useDelimiter("\t");
					String t = null,docs = null;
					while (s.hasNext()) {
						t = s.next();
						docs=(s.hasNext())?s.next():null;
					}
					if (words.contains(t)&&docs!=null) {
						String[] data = docs.split("\\|");
						Vector<Integer> ids=new Vector<Integer>();
						Vector<Vector<Integer>> freqs=new Vector<Vector<Integer>>();
						for(int i=0;i<data.length;i++){
							Scanner s2 = new Scanner(data[i]).useDelimiter(",");							
							int id=s2.nextInt();
							ids.add(id);
							Vector<Integer> pos=new Vector<Integer>();
							while(s2.hasNext()){
								pos.add(s2.nextInt());
							}
							freqs.add(pos);
							s2.close();
						}
						docids.put(t, ids);
						docfreqs.put(t, freqs);
						s.close();
					}
				}
				Vector<Integer> common=new Vector<Integer>(docids.get(terms[0]));
				for(int i=1;i<terms.length;++i){
					common=getCommon(common, docids.get(terms[i]));
				}
				Vector<Integer> finalcommon=new Vector<Integer>();
				int sum=0;
				if(common.size()>0){
					for(Integer id:common){
						int index=docids.get(terms[0]).indexOf(id);
						Vector<Integer> pos=docfreqs.get(terms[0]).get(index);
						for(int i=0;i<terms.length-1;++i){
							int index2=docids.get(terms[i+1]).indexOf(id);
							Vector<Integer> pos2=docfreqs.get(terms[i+1]).get(index2);
							Vector<Integer> newpos=new Vector<Integer>();
							for(Integer p:pos){
								if(pos2.contains(p+1)){
									if(i==terms.length-2){
										sum++;
									}
									newpos.add(p+1);
								}
							}	
							pos=new Vector<Integer>(newpos);
						}
					}
					return sum;
				}else{
					return 0;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}						
		}else{
			try {
				BufferedReader reader = new BufferedReader(new FileReader(indexFile));
				String line;
				int corpusTermFreq = 0;
				out: while ((line = reader.readLine()) != null) {
					Scanner s = new Scanner(line).useDelimiter("\t");
					String t = null,docs = null;
					while (s.hasNext()) {
						t = s.next();
						docs=(s.hasNext())?s.next():null;
					}
					if (t.equals(term)&&docs!=null) {
						String[] data = docs.split("\\|");
						for(int i=0;i<data.length;i++){
							Scanner s2 = new Scanner(data[i]).useDelimiter(",");
							s2.next();
							int o=Integer.parseInt(s2.next());
							//System.out.println(o);
							corpusTermFreq+=o;
							s2.close();
						}
						s.close();
						break out;
					}
					
				}
				reader.close();
				return corpusTermFreq;
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

	@Override
	public int documentTermFrequency(String term, String url) {
		String indexFile = _options._indexPrefix + "/index.idx";
		int docid = 0;
		for(DocumentIndexed doc:_documents){
			if(doc.getUrl().equals(url)){
				docid=doc._docid;
			}else{
				return 0;
			}
		}
		if(term.contains(" ")){
			String[] terms=term.split(" ");
			ArrayList<String> words=(ArrayList<String>) Arrays.asList(terms);
			HashMap<String, Vector<Integer>> freqs=new HashMap<String, Vector<Integer>>();
			BufferedReader reader;
			try {
				reader = new BufferedReader(new FileReader(indexFile));
				String line;
				while ((line = reader.readLine()) != null) {
					Scanner s = new Scanner(line).useDelimiter("\t");
					String t = null,docs = null;
					while (s.hasNext()) {
						t = s.next();
						docs=(s.hasNext())?s.next():null;
					}
					int count=0;
					if (words.contains(t)||docs!=null) {
						count++;
						String[] data = docs.split("\\|");
						for(int i=0;i<data.length;i++){
							Scanner s2 = new Scanner(data[i]).useDelimiter(",");
							int did=s2.nextInt();
							if(did==docid){
								Vector<Integer> f=new Vector<Integer>();
								s2.next();
								while(s2.hasNext()){
									f.add(s2.nextInt());
								}
								freqs.put(t, f);
								break;
							}
							
						}
					}
					if(count==words.size()){
						break;
					}
				}
				Vector<Integer> pos=freqs.get(terms[0]);
				int sum=0;
				for(int i=1;i<terms.length;++i){
					if(pos.size()==0||pos==null){
						break;
					}
					Vector<Integer> newpos=new Vector<Integer>();
					Vector<Integer> pos2=freqs.get(terms[i]);
					for(Integer p:pos){
						if(pos2.contains(p+1)){
							sum++;
							newpos.add(p+1);
						}
					}
					pos=new Vector<Integer>(newpos);
				}
				return sum;
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}else{
			try {
				BufferedReader reader = new BufferedReader(new FileReader(indexFile));
				String line;
				int docTermFreq = 0;
				out: while ((line = reader.readLine()) != null) {
					Scanner s = new Scanner(line).useDelimiter("\t");
					String t = null,docs = null;
					while (s.hasNext()) {
						t = s.next();
						docs=(s.hasNext())?s.next():null;
					}
					if (t.equals(term)||docs!=null) {
						String[] data = docs.split("\\|");
						for(int i=0;i<data.length;i++){
							Scanner s2 = new Scanner(data[i]).useDelimiter(",");
							int did=Integer.parseInt(s2.next());
							if(did==docid){
								docTermFreq+=Integer.parseInt(s2.next());
								s2.close();
								s.close();
								break out;
							}
							s2.close();
						}
					}
					s.close();
				}
				reader.close();
				return docTermFreq;
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return 0;
	}
	
	private int next(String word, int docid, Vector<Integer> docids) {
		if (docids.lastElement() <= docid)
			return -1;
		if (docids.firstElement() > docid) {
			return docids.firstElement();
		}
		int high = docids.size() - 1;
		int result = binarySearch(word, 0, high, docid, docids);
		return docids.get(result);
	}

	private int binarySearch(String word, int low, int high, int docid,Vector<Integer> docIDs) {
		while (high - low > 1) {
			int mid = (low + high) >>> 1;
			if (docIDs.get(mid) <= docid) {
				low = mid + 1;
			} else {
				high = mid;
			}
		}
		return docIDs.get(low) > docid ? low : high;
	}
	
	public static String getContent(String fileName) {  
        /*
		File file = new File(fileName);  
        Long filelength = file.length();  
        byte[] filecontent = new byte[filelength.intValue()];  
        try {  
            FileInputStream in = new FileInputStream(file);  
            in.read(filecontent);  
            in.close();  
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }   */
		StringBuilder content = new StringBuilder();

		try {
			InputStream is = new FileInputStream(fileName);
			InputStreamReader isr = new InputStreamReader(is, "utf-8");
			BufferedReader input = new BufferedReader(isr);
			try {
				String line = null; // not declared within while loop
				while ((line = input.readLine()) != null) {
					content.append(line);
					content.append(System.getProperty("line.separator"));
				}
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
        return Html2Text(content.toString());  
    }  
	public static String Html2Text(String inputString) { 
       /*
		String htmlStr = inputString; 
            String textStr =""; 
      java.util.regex.Pattern p_script; 
      java.util.regex.Matcher m_script; 
      java.util.regex.Pattern p_style; 
      java.util.regex.Matcher m_style; 
      java.util.regex.Pattern p_html; 
      java.util.regex.Matcher m_html; 
   
      try { 
       String regEx_script = "<[\\s]*?script[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?script[\\s]*?>"; 
       String regEx_style = "<[\\s]*?style[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?style[\\s]*?>"; 
          String regEx_html = "<[^>]+>"; 
      
          p_script = Pattern.compile(regEx_script,Pattern.CASE_INSENSITIVE); 
          m_script = p_script.matcher(htmlStr); 
          htmlStr = m_script.replaceAll(""); 

          p_style = Pattern.compile(regEx_style,Pattern.CASE_INSENSITIVE); 
          m_style = p_style.matcher(htmlStr); 
          htmlStr = m_style.replaceAll(""); 
      
          p_html = Pattern.compile(regEx_html,Pattern.CASE_INSENSITIVE); 
          m_html = p_html.matcher(htmlStr); 
          htmlStr = m_html.replaceAll(""); 
      
       textStr = htmlStr; 
      
      }catch(Exception e) { 
               System.err.println("Html2Text: " + e.getMessage()); 
      } 
   
      return textStr;*/
		StringBuilder builder = new StringBuilder();
		String bodyPattern = "<body.*>.+</body>";
		Pattern body = Pattern.compile(bodyPattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		Matcher bodyResult = body.matcher(inputString);
		if(!bodyResult.find()){
			return null;
		}
		String bodyString = bodyResult.group(); //process the body of the html
		// replace all the non-word characters except ' and - to space
		String resultBody = bodyString.replaceAll("\\&.*?;"," ");
		//replace all scripts
		resultBody = resultBody.replaceAll("<script.*?>[\\d\\D]*?</script>"," ");
		//replace all labels
		resultBody = resultBody.replaceAll("</?.*?/?>", " ");
		resultBody = resultBody.replaceAll("[^\\w]", " ");

		// replace duplicate white spaces to one space
		resultBody = resultBody.replaceAll("\\s+"," ");
		if(resultBody.equals("")){
			return null;
		}
		builder.append(resultBody);
		String output = builder.toString();
		Stemmer stemmer = new Stemmer();
		stemmer.add(output);
		stemmer.stem();
		return stemmer.toString();
    }	
	
	public static void main(String[] args) throws IOException,
	ClassNotFoundException {
		Options option = new Options("conf/engine.conf");
		IndexerInvertedOccurrence index = new IndexerInvertedOccurrence(option);
		//index.constructIndex();
		index.loadIndex();
		//
		Query query = new Query("Morrow");
		query.processQuery();
		
		//Document nextdoc = index.nextDoc(query, 346);
		System.out.println(index.corpusTermFrequency("Morrow"));
		/*
		if(nextdoc!=null)
			System.out.println(nextdoc._docid);
		else
			System.out.println("Null");
		*/
	}

}
