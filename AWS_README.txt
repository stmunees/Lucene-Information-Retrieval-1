CS7IS3- Information Retrieval
Tejas Munees, 19312386
README file for the Lucene Program.

The maven project has been deployed.
Five steps has been mentioned below to run the program successfully. All these commands have been tested in the AWS instance successfully.

1. Please go to the directory to build the maven project.

********************************************
cd ~/CS7IS3/Lucene-Information-Retrieval-1/
********************************************

2. Please do mvn clean install.

********************************************
mvn clean install
********************************************

PARAMETERS

3. Once you go to the Jar file location you run the program with these given parameters in the order.

mvn exec:java -Dexec.mainClass="IR.IndexFiles" -Dexec.args= "[-index] [-docs] [-queries] [-score] [-args_path]"
[-index] 	Path to mention where to save your index files after indexing.
[-docs]		Path to the cran.all.1400 file, which is a single file with 1400 docs.
[-queries] 	Path to cran.qry file, which is a single file 225 queries.
[-score]	Score determines the which Similarity Index you want to use.

		0 for ClassicSimilarity
		1 for BM25Similarity (Gives Best MAP Score)
		2 for BooleanSimilarity
		3 for LMDirichletSimilarity
		4 for LMJelinekMercerSimilarity 

[-args_path]	Path to mention where to save the output relevance file.

Example (***THIS ONE WORKS AND PLEASE WAIT FOR A WHILE, AS IT QUERIES ***):

********************************************
mvn exec:java -Dexec.mainClass="IR.IndexFiles" -Dexec.args="-index index -docs Files/cran.all.1400 -queries Files/cran.qry -score 1 -args_path Files/"
********************************************

TREC_EVAL

4. Once you're done with generating the 'outputs.txt' file you are ready to compare with the TREC Eval file. To do that change the directory to the folder which contains the binary file of Trec Eval, as below.

********************************************
cd ~/CS7IS3/Lucene-Information-Retrieval-1/trec_eval-9.0.7
********************************************

5. Now you to run the TREC_EVAL file, run the below code.

********************************************
./trec_eval ~/CS7IS3/Lucene-Information-Retrieval-1/Files/QRelsCorrectedforTRECeval ~/CS7IS3/Lucene-Information-Retrieval-1/Files/outputs.txt
********************************************

For more information, please check "https://github.com/snitch30/Lucene-Information-Retrieval-1".


Code References: 
Lucene Demo Code (IndexFiles.java and SearchFiles.java), 
https://lucene.apache.org/core/8_4_1/demo/overview-summary.html#overview.description. 
Last Accessed 27 Feb 2020.
