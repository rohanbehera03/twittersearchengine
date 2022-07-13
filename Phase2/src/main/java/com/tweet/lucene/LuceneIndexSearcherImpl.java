package com.tweet.lucene;

// import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.document.LongPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuceneIndexSearcherImpl implements LuceneIndexSearcher {
    private final IndexSearcher searcher;
    private int maxResults = 10;

    public LuceneIndexSearcherImpl(final IndexSearcher searcher) {
        this.searcher = searcher;
    }

    Logger logger = LoggerFactory.getLogger(LuceneIndexSearcherImpl.class);

    @Override
    public int getMaxResults() {
        return maxResults;
    }

    @Override
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    @Override
    public Document getIndexedDocument(int documentId) throws IOException {
        return searcher.doc(documentId);
    }

    @Override
    public TopDocs searchById(long id) throws Exception {
        QueryParser parser = new QueryParser("", new StandardAnalyzer());
        Query query = LongPoint.newRangeQuery("id", id, id);
        logger.info("ID is: " + id);
        TopDocs topDocs = searcher.search(query, maxResults);
        logger.info("TopDocs Hits for ID: " + topDocs);
        //List<Document> documents = new ArrayList<>();
        //for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
        //    documents.add(searcher.doc(scoreDoc.doc));
        //}

        return topDocs;
    }

    @Override
    public TopDocs searchByDevice(String deviceStr) throws IOException {
        /*
         * BooleanQuery booleanQuery = new BooleanQuery.Builder().build(); Builder
         * builder = new BooleanQuery.Builder(); String fields[] = { "device" };
         *
         * for (int i = 0; i < fields.length; i++) { TermQuery tq = new TermQuery(new
         * Term(fields[i], deviceStr)); BoostQuery boostQuery = null; if
         * (fields[i].equals("device")) { boostQuery = new BoostQuery(tq, 100.0f); }
         * else { boostQuery = new BoostQuery(tq, 0.0f); } builder.add(new
         * BooleanClause(boostQuery, Occur.SHOULD));
         *
         * }
         *
         * booleanQuery = builder.build(); System.out.println("_?_____?____Query:" +
         * booleanQuery.toString()); TopDocs hits = searcher.search(booleanQuery, 10);
         */
        TermQuery tq = new TermQuery(new Term("device", deviceStr));
        TopDocs hits = searcher.search(tq, maxResults);
        ScoreDoc[] scoreDocs = hits.scoreDocs;

        for (int n = 0; n < scoreDocs.length; ++n) {
            ScoreDoc sd = scoreDocs[n];
            float score = sd.score;
            int docId = sd.doc;
            Document d = searcher.doc(docId);
            String device = d.get("device");
            String text = d.get("text");
            /* x */
            System.out.printf("%3d %4.2f  %s %s\n", n, score, device, text);

        }
        return hits;
    }

    public TopDocs searchByLikesWithBoost(long likes) throws Exception {
        QueryParser qp = new QueryParser("likes", new StandardAnalyzer());
        Query likesQuery = qp.parse(Long.toString(likes));
        TopDocs hits = searcher.search(likesQuery, maxResults);
        return hits;
    }

    @Override
    public TopDocs searchByText(String value) throws Exception {
        QueryParser parser = new QueryParser("text", new StandardAnalyzer());
        Query query = parser.parse(value);

        return searcher.search(query, maxResults);
    }

   @Override
   public TopDocs wildCardSearch(String key, String searchFor) throws Exception {
        Term term = new Term(key, searchFor);

        // create the term query object
        Query query = new WildcardQuery(term);

        System.out.println("\nSearching for '" + query + "' using WildcardQuery");

        // Perform the search
        long startTime = System.currentTimeMillis();

        TopDocs topDocs = searcher.search(query, maxResults);
        long endTime = System.currentTimeMillis();

        System.out.println("wildCardSearch Total Hits: " + topDocs.totalHits.value);
        System.out.println("************************************************************************");
        System.out.println(
                topDocs.totalHits + " documents found. Time taken for the search :" + (endTime - startTime) + "ms");
        System.out.println("************************************************************************");
        System.out.println("");
        return topDocs;

    }
}