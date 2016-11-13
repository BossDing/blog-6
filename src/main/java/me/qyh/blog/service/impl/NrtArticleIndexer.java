/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.service.impl;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TrackingIndexWriter;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.TokenGroup;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleFrom;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;

public abstract class NRTArticleIndexer implements InitializingBean, ApplicationListener<ContextClosedEvent> {

	private static final Logger logger = LoggerFactory.getLogger(NRTArticleIndexer.class);

	private static final Comparator<Article> COMPARATOR = new ArticleCommparator();

	private static final String ID = "id";
	private static final String TITLE = "title";
	private static final String CONTENT = "content";
	private static final String SPACE_ID = "spaceId";
	private static final String PRIVATE = "private";
	private static final String STATUS = "status";
	private static final String FROM = "from";
	private static final String LEVEL = "level";
	private static final String HITS = "hits";
	private static final String COMMENTS = "comments";
	private static final String PUB_DATE = "pubDate";
	private static final String TAG = "tag";
	private static final String LOCKED = "locked";
	private static final String ALIAS = "alias";
	private static final String HIDDEN = "spacePrivate";
	private static final String SUMMARY = "summary";

	protected Analyzer analyzer;
	private final ControlledRealTimeReopenThread<IndexSearcher> reopenThread;
	private final TrackingIndexWriter writer;
	private final ReferenceManager<IndexSearcher> searcherManager;
	private final Directory dir;
	private final IndexWriter oriWriter;

	private Formatter titleFormatter;
	private Formatter tagFormatter;
	private Formatter summaryFormatter;

	private Map<String, Float> boostMap = new HashMap<>();
	private Map<String, Float> qboostMap = new HashMap<>();

	/**
	 * 最大查询数量
	 */
	private static final int MAX_RESULTS = 1000;
	private static final long DEFAULT_COMMIT_PERIOD = 5 * 60 * 1000L;

	private long commitPeriod = DEFAULT_COMMIT_PERIOD;

	@Autowired
	private ThreadPoolTaskScheduler threadPoolTaskScheduler;

	public NRTArticleIndexer(String indexDir, Analyzer analyzer) throws IOException {
		this.dir = FSDirectory.open(Paths.get(indexDir));
		this.analyzer = analyzer == null ? new StandardAnalyzer() : analyzer;
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(OpenMode.CREATE_OR_APPEND);
		try {
			oriWriter = new IndexWriter(dir, config);
			oriWriter.commit();
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		if (commitPeriod <= 0) {
			commitPeriod = DEFAULT_COMMIT_PERIOD;
		}

		writer = new TrackingIndexWriter(oriWriter);
		searcherManager = new SearcherManager(writer.getIndexWriter(), new SearcherFactory());
		reopenThread = new ControlledRealTimeReopenThread<>(writer, searcherManager, 0.5, 0.01);
		reopenThread.setName("Article_Index_NRT");
		reopenThread.setPriority(Math.min(Thread.currentThread().getPriority() + 2, Thread.MAX_PRIORITY));
		reopenThread.setDaemon(true);
		reopenThread.start();
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		close();
	}

	public void close() {
		try {
			reopenThread.close();
			searcherManager.maybeRefreshBlocking();
			writer.getIndexWriter().commit();
			writer.getIndexWriter().close();
			dir.close();
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}

	protected Document buildDocument(Article article) {
		Document doc = new Document();
		doc.add(new StringField(ID, article.getId().toString(), Field.Store.YES));
		doc.add(new TextField(TITLE, article.getTitle(), Field.Store.YES));
		doc.add(new TextField(SUMMARY, clean(article.getSummary()), Field.Store.YES));
		doc.add(new TextField(CONTENT, clean(article.getContent()), Field.Store.YES));
		Set<Tag> tags = article.getTags();
		if (!CollectionUtils.isEmpty(tags))
			for (Tag tag : tags)
				doc.add(new TextField(TAG, tag.getName().toLowerCase(), Field.Store.YES));
		doc.add(new StringField(SPACE_ID, article.getSpace().getId().toString(), Field.Store.NO));
		doc.add(new StringField(PRIVATE, article.isPrivate().toString(), Field.Store.NO));
		doc.add(new StringField(STATUS, article.getStatus().name().toLowerCase(), Field.Store.NO));
		doc.add(new StringField(FROM, article.getFrom().name().toLowerCase(), Field.Store.NO));
		doc.add(new StringField(LOCKED, article.hasLock() ? "true" : "false", Field.Store.NO));
		if (article.getAlias() != null) {
			doc.add(new TextField(ALIAS, article.getAlias(), Field.Store.YES));
		}
		Integer level = article.getLevel();
		doc.add(new NumericDocValuesField(LEVEL, level == null ? -1 : level));
		doc.add(new NumericDocValuesField(HITS, article.getHits()));
		doc.add(new NumericDocValuesField(COMMENTS, article.getComments()));
		String pubDateStr = timeToString(article.getPubDate());
		BytesRef pubDate = new BytesRef(pubDateStr);
		doc.add(new SortedDocValuesField(PUB_DATE, pubDate));
		doc.add(new StringField(PUB_DATE, pubDateStr, Field.Store.NO));
		doc.add(new SortedDocValuesField(ID, new BytesRef(article.getId().toString())));
		Boolean hidden = article.getHidden() == null ? article.getSpace().getArticleHidden() : article.getHidden();
		doc.add(new StringField(HIDDEN, hidden.toString(), Field.Store.NO));
		return doc;
	}

	protected String timeToString(Date date) {
		return DateTools.timeToString(date.getTime(), Resolution.MILLISECOND);
	}

	public void addOrUpdateDocument(Article article) {
		try {
			if (article.hasId()) {
				deleteDocument(article.getId());
			}
			writer.addDocument(buildDocument(article));
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	public void deleteDocument(Integer id) {
		Term term = new Term(ID, id.toString());
		try {
			writer.deleteDocuments(term);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	public List<Article> querySimilar(Article article, ArticlesDetailQuery dquery, int limit) {
		IndexSearcher searcher = null;
		try {
			searcherManager.maybeRefresh();
			searcher = searcherManager.acquire();

			Query likeQuery = buildLikeQuery(article, searcher);
			if (likeQuery == null)
				return Collections.emptyList();
			Builder builder = new Builder();
			builder.add(likeQuery, Occur.MUST);
			builder.add(new TermQuery(new Term(SPACE_ID, article.getSpace().getId().toString())), Occur.MUST);
			TopDocs likeDocs = searcher.search(builder.build(), limit + 1);
			List<Integer> datas = new ArrayList<>();
			for (ScoreDoc scoreDoc : likeDocs.scoreDocs) {
				Document aSimilar = searcher.doc(scoreDoc.doc);
				datas.add(Integer.parseInt(aSimilar.get(ID)));
			}
			if (datas.isEmpty())
				return new ArrayList<>();
			List<Article> articles = dquery.query(datas);
			if (!articles.isEmpty())
				Collections.sort(articles, COMPARATOR);
			List<Article> results = new ArrayList<>();
			int size = 0;
			for (Article art : articles) {
				if (art.equals(article))
					continue;
				results.add(art);
				size++;
				if (size >= limit)
					break;
			}
			return results;
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		} finally {
			try {
				searcherManager.release(searcher);
			} catch (Exception e) {
				//
			}
		}
	}

	protected Query buildLikeQuery(Article article, IndexSearcher searcher) throws IOException {

		TermQuery tq = new TermQuery(new Term(ID, article.getId().toString()));
		TopDocs topDocs = searcher.search(tq, 1);
		int total = topDocs.totalHits;
		if (total > 0) {

			int docId = topDocs.scoreDocs[0].doc;
			IndexReader reader = searcher.getIndexReader();
			MoreLikeThis mlt = new MoreLikeThis(reader);
			mlt.setMinTermFreq(1);
			mlt.setMinDocFreq(1);
			mlt.setMinWordLen(2);
			mlt.setFieldNames(new String[] { TITLE, TAG, ALIAS }); // fields
			mlt.setAnalyzer(analyzer);
			return mlt.like(docId);
		}
		return null;
	}

	public PageResult<Article> query(ArticleQueryParam param, ArticlesDetailQuery dquery) {
		IndexSearcher searcher = null;
		try {
			searcherManager.maybeRefresh();
			searcher = searcherManager.acquire();

			List<SortField> fields = new ArrayList<>();
			if (!param.isIgnoreLevel())
				fields.add(new SortField(LEVEL, Type.INT, true));
			ArticleQueryParam.Sort psort = param.getSort();
			if (psort == null)
				fields.add(SortField.FIELD_SCORE);
			else {
				switch (param.getSort()) {
				case COMMENTS:
					fields.add(new SortField(COMMENTS, Type.INT, true));
					break;
				case HITS:
					fields.add(new SortField(HITS, Type.INT, true));
					break;
				default:
					break;
				}
				fields.add(new SortField(PUB_DATE, SortField.Type.STRING, true));
				fields.add(new SortField(ID, SortField.Type.STRING, true));
			}

			logger.debug(fields.toString());
			Sort sort = new Sort(fields.toArray(new SortField[] {}));

			Builder builder = new Builder();
			Space space = param.getSpace();
			if (space != null) {
				Query query = new TermQuery(new Term(SPACE_ID, space.getId().toString()));
				builder.add(query, Occur.MUST);
			}
			Date begin = param.getBegin();
			Date end = param.getEnd();
			boolean dateRangeQuery = begin != null && end != null;
			if (dateRangeQuery) {
				TermRangeQuery query = new TermRangeQuery(PUB_DATE, new Term(PUB_DATE, timeToString(begin)).bytes(),
						new Term(PUB_DATE, timeToString(end)).bytes(), true, true);
				builder.add(query, Occur.MUST);
			}
			if (!param.isQueryPrivate()) {
				builder.add(new TermQuery(new Term(PRIVATE, "false")), Occur.MUST);
				builder.add(new TermQuery(new Term(LOCKED, "false")), Occur.MUST);
			}
			if (!param.isQueryHidden()) {
				builder.add(new TermQuery(new Term(HIDDEN, "false")), Occur.MUST);
			}
			ArticleFrom from = param.getFrom();
			if (from != null) {
				Query query = new TermQuery(new Term(FROM, from.name().toLowerCase()));
				builder.add(query, Occur.MUST);
			}
			if (param.getTag() != null) {
				builder.add(new TermQuery(new Term(TAG, param.getTag())), Occur.MUST);
			}
			Query multiFieldQuery = null;
			if (param.hasQuery()) {
				String query = MultiFieldQueryParser.escape(param.getQuery().trim());
				MultiFieldQueryParser parser = new MultiFieldQueryParser(
						new String[] { TAG, TITLE, ALIAS, SUMMARY, CONTENT }, analyzer, qboostMap);
				parser.setAutoGeneratePhraseQueries(true);
				parser.setPhraseSlop(0);
				try {
					multiFieldQuery = parser.parse(query);
					builder.add(multiFieldQuery, Occur.MUST);
				} catch (ParseException e) {
				}
			}
			Query query = builder.build();
			logger.debug(query.toString());

			TopDocs tds = searcher.search(query, MAX_RESULTS, sort);
			int total = tds.totalHits;
			int offset = param.getOffset();
			Map<Integer, String> datas = new LinkedHashMap<>();
			if (offset < total) {
				ScoreDoc[] docs = tds.scoreDocs;
				int last = offset + param.getPageSize();
				for (int i = offset; i < Math.min(Math.min(last, total), MAX_RESULTS); i++) {
					int docId = docs[i].doc;
					Document doc = searcher.doc(docId);
					datas.put(Integer.parseInt(doc.get(ID)), doc.get(CONTENT));
				}
			}
			List<Article> articles = dquery.query(new ArrayList<>(datas.keySet()));
			if (param.isHighlight() && multiFieldQuery != null) {
				for (Article article : articles) {
					doHightlight(article, datas.get(article.getId()), multiFieldQuery);
					article.setContent(null);
				}
			}
			return new PageResult<>(param, Math.min(MAX_RESULTS, total), articles);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		} finally {
			try {
				searcherManager.release(searcher);
			} catch (Exception e) {
				//
			}
		}
	}

	/**
	 * 高亮显示
	 * 
	 * @param article
	 *            文章
	 * @param content
	 *            文章内容
	 * @param query
	 */
	protected void doHightlight(Article article, String content, Query query) {
		String titleHL = getHightlight(new Highlighter(titleFormatter, new QueryScorer(query)), TITLE,
				article.getTitle());
		if (titleHL != null)
			article.setTitle(titleHL);
		String summaryHL = getHightlight(new Highlighter(summaryFormatter, new QueryScorer(query)), SUMMARY,
				clean(article.getSummary()));
		if (summaryHL != null)
			article.setSummary(summaryHL);
		else {
			String contentHL = getHightlight(new Highlighter(summaryFormatter, new QueryScorer(query)), CONTENT,
					clean(content));
			if (contentHL != null)
				article.setSummary(contentHL);
		}
		if (!CollectionUtils.isEmpty(article.getTags())) {
			for (Tag tag : article.getTags()) {
				String tagHL = getHightlight(new Highlighter(tagFormatter, new QueryScorer(query)), TAG, tag.getName());
				if (tagHL != null)
					tag.setName(tagHL);
			}
		}

	}

	private String getHightlight(Highlighter highlighter, String fieldName, String text) {
		try {
			return highlighter.getBestFragment(analyzer, fieldName, text);
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	public void deleteAll() {
		try {
			oriWriter.deleteAll();
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	private String clean(String content) {
		return Jsoup.clean(content, Whitelist.none());
	}

	public abstract void removeTag(String... tags);

	public abstract void addTags(String... tags);

	@Override
	public void afterPropertiesSet() throws Exception {
		if (titleFormatter == null)
			titleFormatter = new DefaultFormatter("lucene-highlight-title");
		if (tagFormatter == null)
			tagFormatter = new DefaultFormatter("lucene-highlight-tag");
		if (summaryFormatter == null)
			summaryFormatter = new DefaultFormatter("lucene-highlight-summary");
		qboostMap.put(TAG, boostMap.getOrDefault(TAG, 20F));
		qboostMap.put(ALIAS, boostMap.getOrDefault(ALIAS, 10F));
		qboostMap.put(TITLE, boostMap.getOrDefault(TITLE, 7F));
		qboostMap.put(SUMMARY, boostMap.getOrDefault(SUMMARY, 3F));
		qboostMap.put(CONTENT, boostMap.getOrDefault(CONTENT, 1F));
		if (commitPeriod <= 0)
			commitPeriod = DEFAULT_COMMIT_PERIOD;
		threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
			try {
				oriWriter.commit();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}, commitPeriod);
	}

	public void setTitleFormatter(Formatter titleFormatter) {
		this.titleFormatter = titleFormatter;
	}

	public void setTagFormatter(Formatter tagFormatter) {
		this.tagFormatter = tagFormatter;
	}

	public void setSummaryFormatter(Formatter summaryFormatter) {
		this.summaryFormatter = summaryFormatter;
	}

	private static final class DefaultFormatter implements Formatter {
		private String classes;

		@Override
		public String highlightTerm(String originalText, TokenGroup tokenGroup) {
			return tokenGroup.getTotalScore() <= 0 ? originalText
					: new StringBuilder("<b class=\"").append(classes).append("\">").append(originalText).append("</b>")
							.toString();
		}

		DefaultFormatter(String classes) {
			this.classes = classes;
		}
	}


	private static class ArticleCommparator implements Comparator<Article> {

		@Override
		public int compare(Article o1, Article o2) {
			int compare = -(o1.getPubDate().compareTo(o2.getPubDate()));
			if (compare == 0)
				compare = -o1.getId().compareTo(o2.getId());
			return compare;
		}
	}

	public interface ArticlesDetailQuery {
		List<Article> query(List<Integer> ids);
	}

	public void setBoostMap(Map<String, Float> boostMap) {
		this.boostMap = boostMap;
	}

	public void setCommitPeriod(int commitPeriod) {
		this.commitPeriod = commitPeriod;
	}

}
