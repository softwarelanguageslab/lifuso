The projects in this folder are as follows:

**Project island_parser**

This project contains the grammar and code for our Island Parser in order to extract information from code snippets.
We based our parser in the Java 8 Language Specification [here](https://docs.oracle.com/javase/specs/jls/se8/jls8.pdf).
This project requires a version of Java installed in the system, preferably Java 8.

**Project feature_me**

The project extracts the code and textual information from Stack Overflow (SO) posts, apply the Island Parser and generate vector combinations for each SO answer where is a library usage.


**Project lifuso-backend**

The project processes the output from the previous `feature-me` project by performing clustering, dynamic cut on the dendrogram, selection of clusters, selection of the names of the clusters and finally the generation of the markdown files to be further processed by the UI part of the tool. To execute this project simply execute the following command in the R console: `source("processing.R", echo = T)`.
