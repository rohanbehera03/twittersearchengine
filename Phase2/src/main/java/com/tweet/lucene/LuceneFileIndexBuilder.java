package com.tweet.lucene;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.json.simple.parser.ParseException;

public interface LuceneFileIndexBuilder {
	IndexSearcher createFileIndex(String indexPath) throws IOException, ParseException;

	Document retrieveFullDocument(Document indexedDocument);
	
	
}
