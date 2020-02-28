/*
002 * Licensed to the Apache Software Foundation (ASF) under one or more
003 * contributor license agreements.  See the NOTICE file distributed with
004 * this work for additional information regarding copyright ownership.
005 * The ASF licenses this file to You under the Apache License, Version 2.0
006 * (the "License"); you may not use this file except in compliance with
007 * the License.  You may obtain a copy of the License at
008 *
009 *     http://www.apache.org/licenses/LICENSE-2.0
010 *
011 * Unless required by applicable law or agreed to in writing, software
012 * distributed under the License is distributed on an "AS IS" BASIS,
013 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
014 * See the License for the specific language governing permissions and
015 * limitations under the License.
016 */

package IR;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;

//import org.apache.lucene.search.similarities.*;

/** Simple command-line based search demo. */
public class Searcher {

    private static Object TotalHitCountCollector;

    public Searcher() {}

    /** Simple command-line based search demo. */
    public void SearchMe(String index_paath, String queries_paath, String score_me, String args_paath) throws Exception {
        String usage =
                "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] "+
                        "[-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details."+
                        "[-args_path] for writing directory+ [-score] for score";
//        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
//            System.out.println(usage);
//            System.exit(0);
//        }

        String index = "index";
        String field = "contents";
        String queries = null;
        String write_path = null;
        int repeat = 0;
        boolean raw = true;
        String queryString = null;
        int hitsPerPage = 10;
        int setScore = 0;

        //Setting Parameters
        write_path = args_paath;
        index = index_paath;
        queries = queries_paath;
        setScore = Integer.parseInt(score_me);




        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
        IndexSearcher searcher = new IndexSearcher(reader);
//        searcher.setSimilarity(new BooleanSimilarity());
        if(setScore == 0) searcher.setSimilarity(new ClassicSimilarity());
        if(setScore == 1) searcher.setSimilarity(new BM25Similarity());
        if(setScore == 2) searcher.setSimilarity(new BooleanSimilarity());
        if(setScore == 3) searcher.setSimilarity(new LMDirichletSimilarity());
        if(setScore == 4) searcher.setSimilarity(new LMJelinekMercerSimilarity((float) 0.7));
        //lambda values has to be optimal value is around 0.1 for title queries and 0.7 for long queries.

        Analyzer analyzer = new StandardAnalyzer();

        BufferedReader in = null;
        if (queries != null) {
            in = Files.newBufferedReader(Paths.get(queries), StandardCharsets.UTF_8);
        } else {
            in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        }

        // Giving Different weights to different fields of the search.
        HashMap<String, Float> boostedScores = new HashMap<String, Float>();
        boostedScores.put("Title", 0.65f);
        boostedScores.put("Author", 0.04f);
        boostedScores.put("Bibliography", 0.02f);
        boostedScores.put("Words", 0.35f);
        MultiFieldQueryParser parser = new MultiFieldQueryParser(
                new String[]{"Title", "Author", "Bibliography", "Words"},
                analyzer, boostedScores);


//        QueryParser parser = new QueryParser(field, analyzer);
// Disabling the above due to the presence of multifieldqueryparser.

        String line=in.readLine();
        String nextLine ="";
        int queryNumber = 1;
        PrintWriter writer = new PrintWriter(write_path+"outputs.txt", "UTF-8");
        System.out.println("Please Wait, Querying....");

        while (true) {
            if (queries == null && queryString == null) {                        // prompt the user
                System.out.println("Enter query: ");
            }

            if (line == null || line.length() == -1) {
                break;
            }

            line = line.trim();
            if (line.length() == 0) {
                break;
            }

            //OL Begin
            if( line.substring(0,2).equals(".I") ){
                line = in.readLine();
                if( line.equals(".W") ){
                    line = in.readLine();
                }
                nextLine = "";
                while( !line.substring(0,2).equals(".I") ){
                    nextLine = nextLine + " " + line;
                    line = in.readLine();
                    if( line == null ) break;
                }
            }
            Query query = parser.parse(QueryParser.escape(nextLine.trim()));   //due to an error with the above line (EOF)
            //System.out.println("Searching for: " + query.toString(field));


            if (repeat > 0) {                           // repeat & time as benchmark
                Date start = new Date();
                for (int i = 0; i < repeat; i++) {
                    searcher.search(query, 100);
                }
                Date end = new Date();
                System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
            }

            doPagingSearch(queryNumber, in, searcher, query, hitsPerPage, queries == null && queryString == null, writer);
            queryNumber++;
            if (queryString != null) {
                break;
            }
        }
        writer.close();
        reader.close();
    }



    /**
     * This demonstrates a typical paging search scenario, where the search engine presents
     * pages of size n to the user. The user can then go to the next page if interested in   * the next hits.
     *
     * When the query is executed for the first time, then only enough results are collected
     * to fill 5 result pages. If the user wants to page beyond this limit, then the query
     * is executed another time and all hits are collected.
     *
     */

    public static void doPagingSearch(int queryNumber, BufferedReader in, IndexSearcher searcher, Query query,
                                      int hitsPerPage, boolean interactive, PrintWriter writer) throws IOException {
        TopDocs results = searcher.search(query, 5 * hitsPerPage);
        int numTotalHits = Math.toIntExact(results.totalHits.value);
        results = searcher.search(query, numTotalHits);
        //System.out.println(numTotalHits +" "+ results.totalHits);
        ScoreDoc[] hits = results.scoreDocs;
        //System.out.println(numTotalHits + " total matching documents");
        int start = 0;
        int end = Math.min(numTotalHits, hitsPerPage);

        while (true) {
            end = Math.min(hits.length, start + hitsPerPage);
            for (int i = start; i < numTotalHits; i++) {
                Document doc = searcher.doc(hits[i].doc);
                String path = doc.get("path");
                if (path != null) {
                    //System.out.println(queryNumber + " 0 " + path.replace(".I ","") + " " +(i+1)+ " " + hits[i].score);
                    writer.println(queryNumber+" 0 " + path.replace(".I ","") + " " + (i+1) + " " + hits[i].score +" Any");
                }
            }
            if (!interactive || end == 0) {
                break;
            }
        }
    }
}


// Parameters for the program to run
//-index /Users/stejasmunees/Downloads/lucene-8.4.1/index -docs /Users/stejasmunees/Downloads/lucene-8.4.1/cran/cran.all.1400
// -index /Users/stejasmunees/Downloads/lucene-8.4.1/index -queries /Users/stejasmunees/Downloads/lucene-8.4.1/cran/cran.qry -raw 1 -field contents -args_path /Users/stejasmunees/Downloads/lucene-8.4.1/cran/ -score 1