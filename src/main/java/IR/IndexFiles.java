/*
* Licensed to the Apache Software Foundation (ASF) under one or more
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
//package org.apache.lucene.demo;
package IR;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexFiles {

    private IndexFiles() {}

    /** Index all text files under a directory. */
    public static void main(String[] args) {
        String usage = "java org.apache.lucene.demo.IndexFiles"
                + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                + "in INDEX_PATH that can be searched with SearchFiles";
        String indexPath = "index";
        String docsPath = null;
        String index_paath = null;
        String queries_paath =null;
        String score_me = null;
        String args_paath = null;
        boolean create = true;
        for(int i=0;i<args.length;i++) {
            if ("-index".equals(args[i])) {
                indexPath = args[i+1];
                index_paath = indexPath;
                i++;
            } else if ("-docs".equals(args[i])) {
                docsPath = args[i+1];
                i++;
            }
            else if ("-update".equals(args[i])) {
                create = false;
                i++;
            }
            else if ("-queries".equals(args[i])) {
                queries_paath =args[i+1];
                i++;
            }
            else if ("-score".equals(args[i])) {
                score_me=args[i+1];
                i++;
            }
            else if ("-args_path".equals(args[i])) {
                args_paath=args[i+1];
                i++;
            }
        }

        if (docsPath == null) {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        final Path docDir = Paths.get(docsPath);
        if (!Files.isReadable(docDir)) {
            System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        Date start = new Date();
        try {
            System.out.println("Indexing to directory '" + indexPath + "'...");

            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            if (create) {
                // Create a new index in the directory, removing any
                // previously indexed documents:
                iwc.setOpenMode(OpenMode.CREATE);
            } else {
                // Add new documents to an existing index:
                iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            }

            // Optional: for better indexing performance, if you
            // are indexing many documents, increase the RAM
            // buffer.  But if you do this, increase the max heap
            // size to the JVM (eg add -Xmx512m or -Xmx1g):
            //
            // iwc.setRAMBufferSizeMB(256.0);

            IndexWriter writer = new IndexWriter(dir, iwc);
            indexDocs(writer, docDir);
            writer.close();
            Searcher search = new Searcher();
            try {
                search.SearchMe(index_paath,queries_paath,score_me,args_paath);
                System.out.println("\nIndexing for all 1400 cran documents were successfully done at the directory " + index_paath);
                System.out.println("Searching was successfully performed and 'outputs.txt' was created at " + args_paath);
                System.out.println("You can now use the Trec_Eval to evaluate from the above mentioned 'outputs.txt'.\n");
            } catch (Exception e) {
                e.printStackTrace();
            }

            // NOTE: if you want to maximize search performance,
            // you can optionally call forceMerge here.  This can be
            // a terribly costly operation, so generally it's only
            // worth it when your index is relatively static (ie
            // you're done adding documents to it):
            //
            // writer.forceMerge(1);
            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }

    }

    /**
     * Indexes the given file using the given writer, or if a directory is given,
     * recurses over files and directories found under the given directory.
     *
     * NOTE: This method indexes one document per input file.  This is slow.  For good
     * throughput, put multiple documents into your input file(s).  An example of this is
     * in the benchmark module, which can create "line doc" files, one document per line,
     * using the
     * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
     * >WriteLineDocTask</a>.
     *
     * @param writer Writer to the index where the given file/dir info will be stored
     * @param path The file to index, or the directory to recurse into to find files to index
     * @throws IOException If there is a low-level I/O error
     */
    static void indexDocs(final IndexWriter writer, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException ignore) {
                        // don't index files that can't be read.
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }

    /** Indexes a single document */
    static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {

            BufferedReader inputReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String currentLine = inputReader.readLine();
            Document document;
            String docType = "";

            while (currentLine != null) {
                System.out.print(currentLine);
                //System.out.println("Inside Loop 0"); //For Debugging
                if (currentLine.contains(".I")) {
                    //System.out.println("Inside Loop 1"); //For Debugging
                    document = new Document();
                    Field pathField = new StringField("path", currentLine, Field.Store.YES);
                    document.add(pathField);
                    currentLine = inputReader.readLine();
                    while (!(currentLine.startsWith(".I"))) {
                        //System.out.println("Inside Loop 2"); //For Debugging
                        if (currentLine.startsWith(".T")) {
//                            System.out.println("Inside Loop 3"); //For Debugging
                            docType = "Title";
                            currentLine = inputReader.readLine();

                        } else if (currentLine.startsWith(".A")) {
//                            System.out.println("Inside Loop 4"); //For Debugging
                            docType = "Author";
                            currentLine = inputReader.readLine();
                        } else if (currentLine.startsWith(".W")) {
//                            System.out.println("Inside Loop 5"); //For Debugging
                            docType = "Words";
                            currentLine = inputReader.readLine();
                        } else if (currentLine.startsWith(".B")) {
//                            System.out.println("Inside Loop 6"); //For Debugging
                            docType = "Bibliography";
                            currentLine = inputReader.readLine();
                        }
                        document.add(new TextField(docType, currentLine, Field.Store.YES));
                        currentLine = inputReader.readLine();
                        if (currentLine == null) {
                            break;
                        }
                    }

                    if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
                        // New index, so we just add the document (no old document can be there):
                        System.out.println("adding " + file);
                        writer.addDocument(document);
                    } else {
                        // Existing index (an old copy of this document may have been indexed) so
                        // we use updateDocument instead to replace the old one matching the exact
                        // path, if present:
                        System.out.println("updating " + file);
                        writer.updateDocument(new Term("path", file.toString()), document);
                    }
                }
            }
        }
    }}

// Parameters for the program to run
//-index /Users/stejasmunees/Downloads/lucene-8.4.1/index -docs /Users/stejasmunees/Downloads/lucene-8.4.1/cran/cran.all.1400
//-index /Users/stejasmunees/Downloads/lucene-8.4.1/index -queries /Users/stejasmunees/Downloads/lucene-8.4.1/cran/cran.qry -raw 1 -field contents
