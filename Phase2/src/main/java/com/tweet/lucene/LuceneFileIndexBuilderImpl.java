package com.tweet.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;

public class LuceneFileIndexBuilderImpl implements LuceneFileIndexBuilder {
	private Analyzer analyzer;

	private IndexWriter indexWriter = null;

	public LuceneFileIndexBuilderImpl(){
		super();
		analyzer = new StandardAnalyzer();
	}

	public void cleanupFileIndexFolder(String indexPath) {
		File dir = new File(indexPath);
		final File[] indexFolder = dir.listFiles();
		for (File f : indexFolder)
			f.delete();
	}

	public IndexSearcher createFileIndex(String indexPath) throws IOException, org.json.simple.parser.ParseException {
		// Read from the folder
		Directory indexDir = FSDirectory.open(Paths.get(indexPath));
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(OpenMode.CREATE);
		indexWriter = new IndexWriter(indexDir, config);
		// cleanupFileIndexFolder(INDEX_DIRECTORY);
		forEachTweetFile(this::addDocuments);

		indexWriter.commit();
		indexWriter.close();
		return createSearcher(indexPath);
	}

	// Parse the input JSON file
	public void forEachTweetFile(BiConsumer<String, JSONArray> fileArrayConsumer) {
		URL staticFiles = this.getClass().getClassLoader().getResource("static");
		final Path docDir = Paths.get(staticFiles.getPath());
		if (Files.isDirectory(docDir)) {
			FilenameFilter filter = (f, name) -> {
				// We want to find only .c files
				return name.endsWith(".json");
			};

			File f = new File(staticFiles.getFile());
			File[] files = f.listFiles(filter);
			for (File file : files) {
				InputStream jsonFile;
				try {
					jsonFile = new FileInputStream(file.getAbsolutePath());
					final Reader readerJson = new InputStreamReader(jsonFile);

					// Parse the json file using simple-json library
					Object fileObjects = JSONValue.parse(readerJson);
					JSONArray arrayObjects = (JSONArray) fileObjects;
					fileArrayConsumer.accept(file.getName(), arrayObjects);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}

		}
	}
	public Document retrieveFullDocument(Document indexedDoc) {
		URL staticFiles = this.getClass().getClassLoader().getResource("static");
		String fileRef = indexedDoc.get("file");
		int arrayIdx = (int) indexedDoc.getField("arrayIndex").numericValue();

		File dataDir = new File(staticFiles.getFile());
		File file = new File(dataDir, fileRef);
		try (InputStream jsonFile = Files.newInputStream(Paths.get(file.getAbsolutePath()));
			 Reader readerJson = new InputStreamReader(jsonFile)) {
			 Object fileObjects = JSONValue.parse(readerJson);
			 JSONArray arrayObjects = (JSONArray) fileObjects;
			 return convertJSONObjToDoc((JSONObject) arrayObjects.get(arrayIdx));
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public void addDocuments(String fileName, JSONArray jsonObjects) {
		/*
		 * for (JSONObject object : (List<JSONObject>) jsonObjects) { Document doc = new
		 * Document(); for (String field : (Set<String>) object.keySet()) { Class type =
		 * object.get(field).getClass(); if (type.equals(String.class)) { doc.add(new
		 * StringField(field, (String) object.get(field), Field.Store.YES)); } else if
		 * (type.equals(Long.class)) { doc.add(new LongPoint(field, (long)
		 * object.get(field))); } else if (type.equals(Double.class)) { doc.add(new
		 * DoublePoint(field, (double) object.get(field))); } else if
		 * (type.equals(Boolean.class)) { doc.add(new StringField(field,
		 * object.get(field).toString(), Field.Store.YES)); } } try {
		 * indexWriter.addDocument(doc); } catch (IOException ex) {
		 * System.err.println("Error adding documents to the index. " +
		 * ex.getMessage()); } }
		 */

		for (int arrayIdx = 0; arrayIdx < jsonObjects.size(); ++arrayIdx) {

			Document document = convertJSONObjToDoc((JSONObject) jsonObjects.get(arrayIdx));
			document.add(new StoredField("file", fileName));
			document.add(new StoredField("arrayIndex", arrayIdx));

			try {
				indexWriter.addDocument(document);
			} catch (IOException ex) {
				System.err.println("Error adding documents to the index. " + ex.getMessage());
			}
		}

	}

	private Document convertJSONObjToDoc(JSONObject tweets) {
		Document document = new Document();

		Long id = 0L;
		if (tweets.get("id") != null) {
			id = (Long) tweets.get("id");
		}

		long user_id = 0L;
		if (tweets.get("user_id") != null) {
			user_id = (Long) tweets.get("user_id");
		}

		String created_at = "";
		if (tweets.get("created_at") != null) {
			created_at = (String) tweets.get("created_at");
		}

		String device = "";
		if (tweets.get("device") != null) {
			device = (String) tweets.get("device");
		}

		String text = "";
		if (tweets.get("text") != null) {
			text = (String) tweets.get("text");
		}

		long likes = 0L;
		if (tweets.get("likes") != null) {
			likes = (Long) tweets.get("likes");
		}

		long retweets = 0L;
		if (tweets.get("retweets") != null) {
			likes = (Long) tweets.get("retweets");
		}

		document.add(new LongPoint("id", (Long) id));
		document.add(new LongPoint("user_id", (Long) user_id));
		document.add(new StringField("created_at", created_at, Field.Store.YES));
		document.add(new StringField("device", device, Field.Store.YES));
		document.add(new TextField("text", text, TextField.Store.NO));
		document.add(new LongPoint("likes", (Long) likes));
		document.add(new LongPoint("retweets", (Long) retweets));

		return document;
	}

	private IndexSearcher createSearcher(String indexPath) throws IOException {
		Directory dir = FSDirectory.open(Paths.get(indexPath));
		IndexReader reader = DirectoryReader.open(dir);
		IndexSearcher searcher = new IndexSearcher(reader);
		searcher.setSimilarity(new BM25Similarity());

		return searcher;
	}

}
