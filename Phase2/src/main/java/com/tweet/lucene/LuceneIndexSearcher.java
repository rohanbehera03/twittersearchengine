package com.tweet.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;

public interface LuceneIndexSearcher {

	int getMaxResults();

	void setMaxResults(int maxResults);

	Document getIndexedDocument(int documentId) throws IOException;

	TopDocs searchById(long id) throws Exception;

	TopDocs searchByDevice(String deviceStr) throws Exception;

	TopDocs searchByLikesWithBoost(long likes) throws Exception;

	TopDocs searchByText(String value) throws Exception;

	TopDocs wildCardSearch(String key, String searchFor) throws Exception;

}
