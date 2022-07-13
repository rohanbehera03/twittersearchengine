package com.tweet.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class LuceneSearchWebController {

	@Autowired
	private Environment env;
	private LuceneFileIndexBuilder luceneFileIndexBuilder;
	private LuceneIndexSearcher indexSearcher;

	public LuceneSearchWebController() {
		luceneFileIndexBuilder = new LuceneFileIndexBuilderImpl();

		try {
			IndexSearcher searcher = luceneFileIndexBuilder.createFileIndex("index");
			indexSearcher = new LuceneIndexSearcherImpl(searcher);
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	@Autowired
	private ServletContext context;
	
	Logger logger = LoggerFactory.getLogger(LuceneSearchWebController.class);

	@GetMapping({ "/", "/home" })
	public String home(Model m) {
		m.addAttribute("tweet", new Tweet());
		return "home.html";
	}

	@PostMapping("/home")
	public String homeSubmit(@ModelAttribute("tweet") Tweet tweet, Model model) {

		try { 
			//String indexPath = context.getRealPath(LuceneFileIndexBuilder.indexPath);
			logger.info("Device String: " + tweet.getDevice());

			TopDocs topDocs = indexSearcher.searchByText(tweet.getDevice());
			
			logger.info("Tweet text: " + tweet.getText());
			

			//Thread.sleep(5000);
			long totalHitsVal = topDocs.totalHits.value;
			logger.info("TopDocs Total Hits: " + topDocs.totalHits);
			logger.info("TopDocs Total Hits: " + totalHitsVal);
			//String totalHits = Long.toString(topDocs.totalHits.value);
			//logger.debug("Total Hits: " + totalHits);

			//if 0 result, serch in id field
			if (totalHitsVal == 0) {
				topDocs = indexSearcher.searchById(Long.parseLong(tweet.getDevice()));
				totalHitsVal = topDocs.totalHits.value;
			}

			logger.info("TopDocs Total Hits: " + totalHitsVal);
			model.addAttribute("totalHits", totalHitsVal);
			model.addAttribute("topDocs", topDocs);
			ScoreDoc[] scoreDocs = topDocs.scoreDocs;
			List<Tweet> tweets = new ArrayList<>();
			for(ScoreDoc scoreDoc : scoreDocs) {
				int docId = scoreDoc.doc;
				Document indexedDoc = indexSearcher.getIndexedDocument(docId);
				Document fullDoc = luceneFileIndexBuilder.retrieveFullDocument(indexedDoc);
				//logger.info("print doc: " + fullDoc.toString());
				Tweet aTweet = new Tweet();
				aTweet.setId(docId);
				aTweet.setDevice(fullDoc.get("device"));
				aTweet.setText(fullDoc.get("text"));
				aTweet.setScore(scoreDoc.score);
				tweets.add(aTweet);

			}
			model.addAttribute("tweets", tweets);
			model.addAttribute("scoreDocs", scoreDocs);
		} catch (Exception e) { // TODO
			logger.error(e.getMessage());
			e.printStackTrace();
			return "home";
		}

		return "searchresult";

	}

	@GetMapping("/searchresult")
	public String searchResult(Model model) {

		return "searchresult.html";
	}
}