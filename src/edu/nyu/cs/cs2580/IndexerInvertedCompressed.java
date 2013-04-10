package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import edu.nyu.cs.cs2580.SearchEngine.Options;

public class IndexerInvertedCompressed extends Indexer {
	private static final int vmax = 128;
	static final int con = Integer.MAX_VALUE;
	private String indexFile = _options._indexPrefix + "/compressed/index.idx";
	public String indexFolder= _options._indexPrefix + "/compressed";
	//term did positions
	private Map<Long,Map<Integer,Vector<Integer>>> tmpIndex = new LRUMap<Long,Map<Integer,Vector<Integer>>>(1000,1000);
	//private HashMap<Long,Map<Integer,Vector<Integer>>> tmpIndex = new HashMap<Long,Map<Integer,Vector<Integer>>>();
	private HashMap<String,Integer> CTFCache=new LRUMap<String,Integer>(1000,1000);
	private HashMap<String,TreeSet<Integer>> phraseList=new LRUMap<String,TreeSet<Integer>>(100,100);
	//term did(positions in bytes)
	private HashMap<String, Vector<Vector<Byte>>> _index;
	private Vector<DocumentIndexed> _documents;
	private Map<Integer,Float> pageranks = null;
	private Map<Integer,Integer> numviews = null;
	
	public IndexerInvertedCompressed(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}
	
	@Override
	public Document getDoc(int docid) {
		return _documents.get(docid);
	}

	@Override
	public Document nextDoc(Query query, int docid) {
		Vector<Integer> ids=new Vector<Integer>();
		HashMap<Integer,HashMap<String, Integer>> termfreqs=new HashMap<Integer,HashMap<String, Integer>>();
		for(String q:query._tokens){
			if(q.contains(" ")){
				SortedSet<Integer> keys=null;
				String[] words=q.split(" ");
				if(phraseList.containsKey(q)){
					keys=phraseList.get(q);
				}else{				
					Map<Integer,Vector<Integer>> res = null;
					long hashterm = (long)words[0].hashCode()+(long)con;
					if(tmpIndex.containsKey(hashterm)){
						res = tmpIndex.get(hashterm);
					}else{
						res = getTermLine(hashterm);
						tmpIndex.put(hashterm, res);
					}
					keys = new TreeSet<Integer>(res.keySet());
					//System.out.println("1: "+keys.toString());
					for(int i=1;i<words.length;++i){
						//System.out.println(words[i]);
						Map<Integer,Vector<Integer>> res2 = null;
						long hashterm2 = (long)words[i].hashCode()+(long)con;
						if(tmpIndex.containsKey(hashterm2)){
							res2 = tmpIndex.get(hashterm2);
						}else{
							res2 = getTermLine(hashterm2);
							tmpIndex.put(hashterm2, res2);
						}
						SortedSet<Integer> keys2 = new TreeSet<Integer>(res2.keySet());
						//System.out.println(keys2.toString());
						keys=getCommon(keys, keys2);
					}
					phraseList.put(q, (TreeSet<Integer>) keys);
				}
				//System.out.println("keys:"+keys.toString());
				if(keys.size()==0){
					continue;
				}
				Iterator<Integer> key=keys.iterator();
				while(key.hasNext()){
					int id=key.next();
					if(id<=docid){
						continue;
					}
					int sum=0;
					boolean contains=false;
					long hash = (long)words[0].hashCode()+(long)con;
					Vector<Integer> pos=tmpIndex.get(hash).get(id);
					for(int i=1;i<words.length;++i){
						long hash2 = (long)words[i].hashCode()+(long)con;
						Vector<Integer> pos2=tmpIndex.get(hash2).get(id);
						Vector<Integer> newpos=new Vector<Integer>();
						for(Integer p:pos){
							if(pos2.contains(p+1)){
								newpos.add(p+1);
								if(i==words.length-1){
									sum++;
									contains=true;
								}
							}
						}
						pos=new Vector<Integer>(newpos);
					}
					if(contains){
						HashMap<String, Integer> freq=new HashMap<String, Integer>();
						freq.put(q, sum);
						termfreqs.put(id, freq);
						ids.add(id);
						break;
					}
				}
			}
		}
		int id;
		for(int i=0;i<query._tokens.size();i++){
			if(query._tokens.get(i).contains(" ")){
				continue;
			}
			id=next(query._tokens.get(i),docid);
			 // only add the id that exists
			if(id != -1 )
				ids.add(id);  
		}
		//System.out.println("size:"+ids.size());
		   // return null if no document contains any term of the query or when couldn't find any document that contains that term
	   if(ids.size()==0 || ids.size()!=query._tokens.size()){
		   return null;
	   }else if(find(ids)){ 
		   DocumentIndexed d=(DocumentIndexed) getDoc(ids.get(0));
		   Vector<Integer> tfreqs=new Vector<Integer>();
		   for(String q:query._tokens){
				if(q.contains(" ")){
					tfreqs.add(termfreqs.get(d._docid).get(q));
				}else{
					Map<Integer,Vector<Integer>> res = null;
					int freq = 0;
					long hashterm = (long)q.hashCode()+(long)con;
					if(tmpIndex.containsKey(hashterm)){
						res = tmpIndex.get(hashterm);
					}else{
						res = getTermLine(hashterm);
						tmpIndex.put(hashterm, res);
					}
					if(res.containsKey(ids.get(0)))
						freq = res.get(ids.get(0)).size();
					tfreqs.add(freq);
				}
		   }
		   d.setDocumentTermFrequency(tfreqs);
		   return d;
	   }
	   else{
		  return nextDoc(query, max(ids)-1); 
	   }
	}
	
	private SortedSet<Integer> getCommon(Set<Integer> v1, Set<Integer> v2){
		SortedSet<Integer> result=new TreeSet<Integer>();
		Iterator<Integer> it1 = v1.iterator();
		Iterator<Integer> it2 = v2.iterator();
		int a=it1.next();
		int b=it2.next();
		while(it1.hasNext()&&it2.hasNext()){
			if(a==b){
				result.add(a);
				a=it1.next();
				b=it2.next();
			}else if(a>b){
				b=it2.next();
			}else{
				a=it1.next();
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
	
	@SuppressWarnings("unchecked")
	@Override
	public void constructIndex() throws IOException {
		/*
		CorpusAnalyzer ca = new CorpusAnalyzerPagerank(_options);
	  	pageranks = (HashMap<Integer,Float>)ca.load();
	  	LogMinerNumviews log = new LogMinerNumviews(_options);
	  	numviews = (HashMap<Integer,Integer>)log.load();
		*/
		_index =new HashMap<String,Vector<Vector<Byte>>>();
		this._totalTermFrequency=0;
		this._numDocs=0;
		String corpus = _options._corpusPrefix + "/";
		System.out.println("Construct index from: " + corpus);
		int count=0;
		int did=0;
		SortedSet<String> allterms=new TreeSet<String>();
		File root = new File(corpus);
        File[] files = root.listFiles();
        String documents = _options._indexPrefix + "/compressed/documents.idx";
        File folder=new File(_options._indexPrefix + "/compressed");
        if(!folder.exists()){
        	folder.mkdir();
        }
		BufferedWriter out2 = new BufferedWriter(new FileWriter(documents));
        for (int i = 0; i < files.length; i++) {
        	HashMap<String, Vector<Byte>> positions = new HashMap<String, Vector<Byte>>();
        	HashMap<String, Integer> freqs = new HashMap<String, Integer>();
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
        	Vector<Byte> v_did = vbyteConversion(did);
  		  	Vector<Byte> enposition = null;
        	while(s.hasNext()){
        		String word=s.next();
        		//System.out.println(word);
        		if(word==""||word==" "||word.length()==0){
        			continue;
        		}
        		++_totalTermFrequency;
        		allterms.add(word);
				if(!positions.containsKey(word)){
					enposition = new Vector<Byte>();
					enposition.addAll(v_did);
					Vector<Byte> v_pos = vbyteConversion(p);
					enposition.addAll(v_pos);
					positions.put(word, enposition);
				}else{
					enposition=positions.get(word);
					Vector<Byte> v_pos = vbyteConversion(p);
			    	enposition.addAll(v_pos);
				}
				if(freqs.containsKey(word)){
					int f=freqs.get(word);
					freqs.put(word, f+1);
				}else{
					freqs.put(word, 1);
				}
				p++;
        	}
        	s.close();
        	String filePath = _options._indexPrefix + "/compressed/documents/" + did+".idx";
        	File docfolder = new File(_options._indexPrefix + "/compressed/documents");
    		if (!docfolder.exists()) {
    			docfolder.mkdir();
    		}
    		FileOutputStream out3 = new FileOutputStream(filePath);
        	for (String term : positions.keySet()) {
    			if (!_index.containsKey(term)) {
    				Vector<Vector<Byte>> doc_pos = new Vector<Vector<Byte>>();
    				doc_pos.add(positions.get(term));
    				_index.put(term, doc_pos);
    			} else {
    				Vector<Vector<Byte>> doc_pos = _index.get(term);
    				doc_pos.add(positions.get(term));
    			}
    			byte[] v_term_count = vbyteConversionToArray(freqs.get(term));
    			long hash_term = (long)term.hashCode()+(long)con;
    			byte[] v_hash_term = vbyteConversionToArray(hash_term);
    			out3.write(v_hash_term);
    			out3.write(v_term_count);
    		}
        	out3.close();
			//out3.flush();
			out3.close();
        	//doc.bodySize=p-1;
			/*
			float pagerank = 0;
		    if(this.pageranks.containsKey(did))
		    	pagerank = this.pageranks.get(did);
		    int views = 0;
		    if(this.numviews.containsKey(did))
		    	views = this.numviews.get(did);
		    	*/
        	//out2.write(did+"\t"+files[i].getName()+"\t"+files[i].getName()+"\t"+(p-1)+"\t"+pagerank+"\t"+views);
			out2.write(did+"\t"+files[i].getName()+"\t"+files[i].getName()+"\t"+(p-1));
        	out2.newLine();
        	//out2.flush();
        	//_documents.add(doc);
        	did++;
        	if((i/200)>count||(i==files.length-1)){
        		String foldername = _options._indexPrefix+"/compressed";
        		File tmpfolder = new File(foldername);
    			if (!tmpfolder.isDirectory()) {
    				tmpfolder.mkdir();
    			}
    			File tmpfile = new File(foldername+"/tmp"+count+".idx");
    			//FileWriter fileWritter = new FileWriter(tmpfile);
    			//BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
    			FileOutputStream fos=new FileOutputStream(tmpfile);
        		SortedSet<String> keys = new TreeSet<String>(_index.keySet());
        		//HashMap<Long,Vector<Byte>> map = new HashMap<Long,Vector<Byte>>();
        		for (String term : keys) {
        			long termhash = (long)term.hashCode()+(long)con;
        			fos.write(vbyteConversionToArray(termhash));
        			//Vector<Byte> finalbytes = new Vector<Byte>();
        			int totalsize=0;
        			for(Vector<Byte> bytes:_index.get(term)){
        				int size = bytes.size();
        				byte[] byteSize=vbyteConversionToArray(size);
        				totalsize+=size;
        				totalsize+=byteSize.length;
        			}
        			fos.write(vbyteConversionToArray(totalsize));
        			for(Vector<Byte> bytes:_index.get(term)){
        				int size = bytes.size();
  					  	fos.write(vbyteConversionToArray(size));//size of bytes of all numbers of a doc
  					  	byte[] termPosContent = new byte[size];
  					  	for(int j = 0; j<bytes.size(); j++){
  					  		termPosContent[j] = bytes.get(j);
  					  	}
  					  	fos.write(termPosContent);
        			}
        			//map.put(termhash, finalbytes);
        		}
        		fos.close();
        		_index.clear();
        		count++;
        	}
        }
        this._numDocs=did;
        String statistics = _options._indexPrefix + "/compressed/statistics.idx";
		BufferedWriter out = new BufferedWriter(new FileWriter(statistics));
		out.write(""+_numDocs);
		out.newLine();
		out.write(""+_totalTermFrequency);
		out.flush();
		out.close();
		//String doc = _options._indexPrefix + "/compressed/documents.idx";
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
		String foldername = _options._indexPrefix+"/compressed";
		//SortedSet<Integer> ids=new TreeSet<Integer>();
		HashMap<Integer, byte[]> currentLine=new HashMap<Integer, byte[]>();
		//termlinesize
		HashMap<Integer, byte[]> currentTLS=new HashMap<Integer, byte[]>();
		HashMap<Integer, Long> currentTerm=new HashMap<Integer, Long>();
		File root = new File(foldername);
        File[] files = root.listFiles();
        FileInputStream[] readers = new FileInputStream[files.length-3];
        for (int i = 0; i < files.length-3; i++) {
        	readers[i] = new FileInputStream(foldername+"/tmp"+i+".idx");
        }
        File index = new File(foldername+"/index.idx");
		if (!index.exists()) {
			index.createNewFile();
		}
		FileOutputStream indexWritter = new FileOutputStream(foldername+"/index.idx");
		/*
		File log = new File(foldername+"/log.txt");
		if (!log.exists()) {
			log.createNewFile();
		}
		FileWriter lWritter = new FileWriter(log);
		BufferedWriter logWritter = new BufferedWriter(lWritter);*/
        Iterator<String> it = allterms.iterator();
        while(it.hasNext()){
        	String term=(String)it.next();
        	long termHash = (long)term.hashCode()+(long)con;        	
        	indexWritter.write(vbyteConversionToArray(termHash));
        	Vector<byte[]> newLines=new Vector<byte[]>();
        	Vector<byte[]> TLS=new Vector<byte[]>();
        	//logWritter.write("["+term+"]");
        	//logWritter.newLine();
        	for (int i = 0; i < files.length-3; i++) {
        		//logWritter.write("file: "+i);
        		//logWritter.newLine();
        		byte[] line,curTLS;
        		long currentTermHash;
        		boolean stored=false;
        		if(currentLine.containsKey(i)){
        			line=currentLine.get(i);
        			currentTermHash=currentTerm.get(i);
        			curTLS=currentTLS.get(i);
        			stored=true;
        			//logWritter.write("get saved line");
        			//logWritter.newLine();
        		}else{
        			Vector<Byte> t= getNextWholeNumber(readers[i]);
        			if(t.size()==0){
        				continue;
        			}
        			currentTermHash = convertVbyteToNumLong(t);
        			Vector<Byte> termLineSize=getNextWholeNumber(readers[i]);
        			curTLS=new byte[termLineSize.size()];
        			for(int x=0;x<curTLS.length;++x){
        				curTLS[x]=termLineSize.get(x);
        			}
        			int byteLength = convertVbyteToNum(termLineSize);
        			line = new byte[byteLength];
        			readers[i].read(line);
        			//logWritter.write("read in new line: ");
        			//logWritter.newLine();
        		}
        		if(line != null){
        			//logWritter.write("term: "+term+" hash: "+termHash+" curhash:"+currentTermHash);
        			//logWritter.write(currentTermHash==termHash?"  =":"    !=");
        			//logWritter.newLine();
        			if(currentTermHash==termHash){
        				//indexWritter.write(curTLS);
        				//indexWritter.write(line);
        				TLS.add(curTLS);
        				newLines.add(line);
        				//logWritter.write("write in index");
            			//logWritter.newLine();
        				if(stored){
        					currentLine.remove(i);
        					//logWritter.write("remove saved line");
        					//logWritter.newLine();
        				}
        			}else{
        				if(!stored){
        					currentLine.put(i, line);
        					currentTerm.put(i, currentTermHash);
        					currentTLS.put(i, curTLS);
        					//logWritter.write("save line");
        					//logWritter.newLine();
        				}
        			}
        		}
        	}
        	int totalsize=0;
        	for(int i=0;i<TLS.size();++i){
        		int tls=convertVbyteToNum(TLS.get(i));
        		totalsize+=tls;
        	}
        	indexWritter.write(vbyteConversionToArray(totalsize));
        	for(int i=0;i<newLines.size();++i){
        		indexWritter.write(newLines.get(i));
        	}
        }
        for (int i = 0; i < files.length-3; i++) {
        	readers[i].close();
        	File file = new File(foldername+"/tmp"+i+".idx");
        	file.delete(); 
        }
        //indexWritter.flush();
        indexWritter.close();
        //logWritter.flush();
        //logWritter.close();
	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		String stat = _options._indexPrefix + "/compressed/statistics.idx";
		String docFile = _options._indexPrefix + "/compressed/documents.idx";
		_documents=new Vector<DocumentIndexed>();
		System.out.println("Load index from: " + indexFile);
		BufferedReader reader = new BufferedReader(new FileReader(stat));
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
			//doc.setPageRank(Float.parseFloat(l[4]));
			//doc.setNumViews(Integer.parseInt(l[5]));
			_documents.add(doc);			
		}
		reader.close();
	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		long hashterm = (long)term.hashCode()+(long)con;
		Map<Integer,Vector<Integer>> res = null;
		if(tmpIndex.containsKey(hashterm)){
			res = tmpIndex.get(hashterm);
		}
		else{
			res = getTermLine(hashterm);
			tmpIndex.put(hashterm, res);
		}
		return res.size();
	}

	@Override
	public int corpusTermFrequency(String term) {
		if (CTFCache.containsKey(term)) {
			return CTFCache.get(term);
		}
		if(term.contains(" ")){
			String[] words=term.split(" ");
			SortedSet<Integer> keys=null;
			if(phraseList.containsKey(term)){
				keys=phraseList.get(term);
			}else{				
				Map<Integer,Vector<Integer>> res = null;
				long hashterm = (long)words[0].hashCode()+(long)con;
				if(tmpIndex.containsKey(hashterm)){
					res = tmpIndex.get(hashterm);
				}else{
					res = getTermLine(hashterm);
					tmpIndex.put(hashterm, res);
				}
				keys = new TreeSet<Integer>(res.keySet());
				for(int i=1;i<words.length;++i){
					Map<Integer,Vector<Integer>> res2 = null;
					long hashterm2 = (long)words[i].hashCode()+(long)con;
					if(tmpIndex.containsKey(hashterm2)){
						res2 = tmpIndex.get(hashterm2);
					}else{
						res2 = getTermLine(hashterm2);
						tmpIndex.put(hashterm2, res2);
					}
					SortedSet<Integer> keys2 = new TreeSet<Integer>(res2.keySet());
					keys=getCommon(keys, keys2);
				}
			}
			if(keys.size()==0){
				return 0;
			}
			int sum=0;
			Iterator<Integer> key=keys.iterator();
			while(key.hasNext()){
				int id=key.next();
				long hash = (long)words[0].hashCode()+(long)con;
				Vector<Integer> pos=tmpIndex.get(hash).get(id);
				for(int i=1;i<words.length;++i){
					long hash2 = (long)words[i].hashCode()+(long)con;
					Vector<Integer> pos2=tmpIndex.get(hash2).get(id);
					Vector<Integer> newpos=new Vector<Integer>();
					for(Integer p:pos){
						if(pos2.contains(p+1)){
							newpos.add(p+1);
							if(i==words.length-1){
								sum++;
							}
						}
					}
					pos=new Vector<Integer>(newpos);
				}
			}
			CTFCache.put(term, sum);
			return sum;
		}else{
			long hashterm = (long)term.hashCode()+(long)con;
			Map<Integer,Vector<Integer>> res = null;
			//Map<Integer,Vector<Integer>> res = getTerm((long)term.hashCode()+(long)con);
			if(tmpIndex.containsKey(hashterm)){
				res = tmpIndex.get(hashterm);
			}else{
				res = getTermLine(hashterm);
				tmpIndex.put(hashterm, res);
			}
			int total=0;
			for(int did:res.keySet()){
				total += res.get(did).size();
			}
			CTFCache.put(term, total);
			return total;
		}
	}

	@Override
	public int documentTermFrequency(String term, String url) {
		int did=-1;
		for(DocumentIndexed d:_documents){
			if(d.getUrl().equals(url)){
				did=d._docid;
			}
		}
		if(did==-1){
			return 0;
		}
		Map<Integer,Vector<Integer>> res = null;
		int freq = 0;
		long hashterm = (long)term.hashCode()+(long)con;
		if(tmpIndex.containsKey(hashterm)){
			res = tmpIndex.get(hashterm);
		}else{
			res = getTermLine(hashterm);
			tmpIndex.put(hashterm, res);
		}
		if(res.containsKey(did))
			freq = res.get(did).size();
		return freq;
	}
	
	public int documentTermFrequency(String term, int id) {
		return 0;
	}
	
	private int next(String word, int docid) {
		long hashword = (long)word.hashCode()+(long)con;
		Map<Integer,Vector<Integer>> res = null;
		if(tmpIndex.containsKey(hashword)){
			 res = tmpIndex.get(hashword);
		}
		else{
			res = getTermLine(hashword);
			tmpIndex.put(hashword, res);
		}
		if(res.size()==0){
			return -1;
		}
		TreeSet<Integer> keySet = new TreeSet<Integer>(res.keySet());
		Integer nextdoc = keySet.higher(docid);
	    return nextdoc==null? -1:nextdoc;
	}
	
	public Vector<Byte> vbyteConversion(int num){
		  Vector<Byte> num_to_bytes = new Vector<Byte>();
		  boolean firstByte = true;
		  if(num == 0)
			  num_to_bytes.add((byte)(1<<7));
		  while(num>0)
		  {
			  byte bytenum = (byte)(num % vmax);
			  num = num >> 7;
			  if (firstByte)
			  {
				  // indicate the end of a byte, set the hightest bit to 1
				  bytenum |= 1 << 7;
				  firstByte = false;
			  }
			  num_to_bytes.add(bytenum);	  
		  }
		  Collections.reverse(num_to_bytes);
		  return num_to_bytes;
	  }

	public byte[] vbyteConversionToArray(int num){
		  if(num == 0){
			  byte[] res = new byte[1];
			  res[0] = (byte)(1<<7);
			  return res;
		  }
		  int count = 0;
		  int temp = num;
		  boolean firstByte = true;
		  while(temp>0){
			  ++ count;
			  temp = temp >> 7;
		  }
		  byte[] res = new byte[count];
		  int i =0;
		  while(num > 0){
			  byte bytenum = (byte)(num % vmax);
			  num = num >> 7;
			  if (firstByte)
			  {
				  // indicate the end of a byte, set the hightest bit to 1
				  bytenum |= 1 << 7;
				  firstByte = false;
			  }
			  res[count-1-i] = bytenum;
			  i++;
		  }
		  return res;
	  }
	
	public byte[] vbyteConversionToArray(long num){
		  if(num == 0){
			  byte[] res = new byte[1];
			  res[0] = (byte)(1<<7);
			  return res;
		  }
		  int count = 0;
		  long temp = num;
		  boolean firstByte = true;
		  while(temp>0){
			  ++ count;
			  temp = temp >> 7;
		  }
		  byte[] res = new byte[count];
		  int i =0;
		  while(num > 0){
			  byte bytenum = (byte)(num % vmax);
			  num = num >> 7;
			  if (firstByte){
				  // indicate the end of a byte, set the hightest bit to 1
				  bytenum |= 1 << 7;
				  firstByte = false;
			  }
			  res[count-1-i] = bytenum;
			  i++;
		  }
		  return res;
	  }
	
	public int convertVbyteToNum(byte[] vbyte){
		  if(vbyte.length == 0)
			  return -1;
		  int res = 0;
		  res += (long)(vbyte[vbyte.length-1] & ((1<<7)-1));
		  for(int i=vbyte.length-2;i>=0;--i)
			  res+=vbyte[i]*((int)Math.pow(vmax, (vbyte.length-1-i)));
		  return res;
	}
	
	public int convertVbyteToNum(Vector<Byte> vbyte){
		  Collections.reverse(vbyte);
		  if (vbyte == null || vbyte.size() == 0)
			  return -1;
		  int res=0;
		  res += (int) (vbyte.get(0) & ((1<<7)-1)) ;
		  for(int i=1;i<vbyte.size();i++)
		  {
			  res+=vbyte.get(i)*((int)Math.pow(vmax, i));
		  }		  
		  return res;
	}
	
	public long convertVbyteToNumLong(byte[] vbyte){
		  if(vbyte.length == 0)
			  return -1;
		  long res = 0;
		  res += (long)(vbyte[vbyte.length-1] & ((1<<7)-1));
		  for(int i=vbyte.length-2;i>=0;--i)
			  res+=vbyte[i]*((long)Math.pow(vmax, (vbyte.length-1-i)));
		  return res;
	}
	
	public long convertVbyteToNumLong(Vector<Byte> vbyte){
		  Collections.reverse(vbyte);
		  if (vbyte == null || vbyte.size() == 0)
			  return -1;
		  long res=0;
		  res += (long) (vbyte.get(0) & ((1<<7)-1)) ;
		  for(int i=1;i<vbyte.size();i++)
		  {
			  res+=vbyte.get(i)*((long)Math.pow(vmax, i));
		  }		  
		  return res;
	}
	
	private Vector<Byte> getNextWholeNumber(FileInputStream fis){
		  Vector<Byte> currentNumber = new Vector<Byte>();
		  int current;
		  try{
			while((current = fis.read())!=-1){
				  currentNumber.add((byte)current);
				  if((current & (1<<7)) > 0){
					  //the current byte is the ending byte
					  break;
				  }
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return currentNumber;
	}
	
	private Vector<Vector<Byte>> getNumbers(byte[] input){
		  Vector<Byte> currentNumber = new Vector<Byte>();
		  Vector<Vector<Byte>> ret = new Vector<Vector<Byte>>();
		  byte current;
		  int index = 0;
			while(index < input.length){
				current = input[index++];
				currentNumber.add((byte)current);
				if((current & (1<<7)) > 0){
					//the current byte is the ending byte
					ret.add(currentNumber);
					currentNumber = new Vector<Byte>();
				}
			}
		return ret;
	  }
	
	private Map<Integer, Vector<Integer>> getTermLine(long termHash){
		  Map<Integer, Vector<Integer>> ret = new HashMap<Integer, Vector<Integer>>();
			try {
				FileInputStream fis = new FileInputStream(indexFile);
				long currentTerm;
				while((currentTerm = convertVbyteToNumLong(getNextWholeNumber(fis)))!= -1){
					int termLineLength = convertVbyteToNum(getNextWholeNumber(fis));
					//System.out.println(currentTerm);
					if(currentTerm != termHash){
						//System.out.println(termLineLength);
						fis.skip((long)termLineLength);
					}
					else{
						while(termLineLength > 0){
							Vector<Byte> lengthByte = getNextWholeNumber(fis);
							int docLength = convertVbyteToNum(lengthByte);
							byte[] doc = new byte[docLength];
							fis.read(doc);
							Vector<Vector<Byte>> numbers = getNumbers(doc);
							int docId = convertVbyteToNum(numbers.get(0));
							Vector<Integer> positions = new Vector<Integer>();
							for(int i = 1; i<numbers.size(); i++){
								positions.add(convertVbyteToNum(numbers.get(i)));
							}
							//System.out.println("id:"+docId+" freq: "+numbers.size());
							//Add the doc and positions to the map
							ret.put(docId, positions);
							termLineLength-=(docLength + lengthByte.size());
						}
						break;
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
			return ret;
	  }
	
	public static String getContent(String fileName) {  
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
		IndexerInvertedCompressed index = new IndexerInvertedCompressed(option);
		//index.constructIndex();
		index.loadIndex();
		
		Query query = new Query("Morrow");
		query.processQuery();
		
		DocumentIndexed nextdoc = (DocumentIndexed) index.nextDoc(query, -1);
		System.out.println(nextdoc._docid+" "+nextdoc.bodySize);
		nextdoc = (DocumentIndexed) index.nextDoc(query, nextdoc._docid);
		System.out.println(nextdoc._docid+" "+nextdoc.bodySize);
		//System.out.println(index.corpusTermFrequency("Web"));
		/*
		if(nextdoc!=null)
			System.out.println(nextdoc._docid);
		else
			System.out.println("Null");
		*/
	}

}
