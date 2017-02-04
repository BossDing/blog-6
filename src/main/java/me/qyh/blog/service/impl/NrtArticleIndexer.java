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
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleFrom;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;

/**
 * 近实时文章索引
 * <p>
 * 通过配置commitPeriod可以设置commit频率
 * </p>
 * 
 * @author Administrator
 *
 */
public abstract class NRTArticleIndexer implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(NRTArticleIndexer.class);

	private static final Comparator<Article> COMPARATOR = Comparator.comparing(Article::getPubDate).reversed()
			.thenComparing(Comparator.comparing(Article::getId).reversed());

	private static final String ID = "id";
	private static final String TITLE = "title";
	private static final String CONTENT = "content";
	private static final String SPACE_ID = "spaceId";
	private static final String PRIVATE = "private";
	private static final String STATUS = "status";
	private static final String FROM = "from";
	private static final String LEVEL = "level";
	private static final String HITS = "hits";
	private static final String PUB_DATE = "pubDate";
	private static final String TAG = "tag";
	private static final String LOCKED = "locked";
	private static final String ALIAS = "alias";
	private static final String SUMMARY = "summary";
	private static final String LASTMODIFYDATE = "lastModifyDate";

	protected Analyzer analyzer;
	private final ControlledRealTimeReopenThread<IndexSearcher> reopenThread;
	private final TrackingIndexWriter writer;
	private final ReferenceManager<IndexSearcher> searcherManager;
	private final Directory dir;
	private final IndexWriter oriWriter;

	private Formatter titleFormatter;
	private Formatter tagFormatter;
	private Formatter summaryFormatter;

	private Map<String, Float> boostMap = Maps.newHashMap();
	private Map<String, Float> qboostMap = Maps.newHashMap();

	/**
	 * 最大查询数量
	 */
	private static final int MAX_RESULTS = 1000;
	private static final long DEFAULT_COMMIT_PERIOD = 30 * 60 * 1000L;

	private long commitPeriod = DEFAULT_COMMIT_PERIOD;

	@Autowired
	private ThreadPoolTaskScheduler threadPoolTaskScheduler;
	@Autowired(required = false)
	private ArticleContentHandler articleContentHandler;

	/**
	 * 构造器
	 * 
	 * @param indexDir
	 *            索引文件目录
	 * @param analyzer
	 *            分析器
	 * @throws IOException
	 *             索引目录打开失败等
	 * 
	 */
	public NRTArticleIndexer(String indexDir, Analyzer analyzer) throws IOException {
		this.dir = FSDirectory.open(Paths.get(indexDir));
		this.analyzer = analyzer;
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(OpenMode.CREATE_OR_APPEND);
		try {
			oriWriter = new IndexWriter(dir, config);
			oriWriter.commit();
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}

		writer = new TrackingIndexWriter(oriWriter);
		searcherManager = new SearcherManager(writer.getIndexWriter(), new SearcherFactory());
		reopenThread = new ControlledRealTimeReopenThread<>(writer, searcherManager, 0.5, 0.01);
		reopenThread.setName("Article_Index_NRT");
		reopenThread.setPriority(Math.min(Thread.currentThread().getPriority() + 2, Thread.MAX_PRIORITY));
		reopenThread.setDaemon(true);
		reopenThread.start();
	}

	public void close() {
		try {
			searcherManager.maybeRefreshBlocking();
			reopenThread.close();
			writer.getIndexWriter().commit();
			writer.getIndexWriter().close();
			dir.close();
		} catch (IOException e) {
			LOGGER.warn(e.getMessage(), e);
		}
	}

	protected Document buildDocument(Article article) {
		Document doc = new Document();
		doc.add(new StringField(ID, article.getId().toString(), Field.Store.YES));
		doc.add(new TextField(TITLE, article.getTitle(), Field.Store.YES));
		doc.add(new TextField(SUMMARY, clean(article.getSummary()), Field.Store.YES));
		doc.add(new TextField(CONTENT, cleanContent(article), Field.Store.YES));
		Set<Tag> tags = article.getTags();
		if (!CollectionUtils.isEmpty(tags)) {
			for (Tag tag : tags) {
				doc.add(new TextField(TAG, tag.getName().toLowerCase(), Field.Store.YES));
			}
		}
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
		String pubDateStr = timeToString(article.getPubDate());
		BytesRef pubDate = new BytesRef(pubDateStr);
		doc.add(new SortedDocValuesField(PUB_DATE, pubDate));
		doc.add(new StringField(PUB_DATE, pubDateStr, Field.Store.NO));

		Timestamp lastModifyDate = article.getLastModifyDate();
		if (lastModifyDate != null) {
			doc.add(new SortedDocValuesField(LASTMODIFYDATE, new BytesRef(timeToString(lastModifyDate))));
		}
		doc.add(new SortedDocValuesField(ID, new BytesRef(article.getId().toString())));
		return doc;
	}

	protected String timeToString(Date date) {
		return DateTools.timeToString(date.getTime(), Resolution.MILLISECOND);
	}

	/**
	 * 增加|更新文章索引，如果文章索引存在，则先删除后增加索引
	 * 
	 * @param article
	 *            要增加|更新索引的文章
	 */
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

	/**
	 * 删除索引
	 * 
	 * @param id
	 *            文章id
	 */
	public void deleteDocument(Integer id) {
		Term term = new Term(ID, id.toString());
		try {
			writer.deleteDocuments(term);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	/**
	 * 查询相似文章
	 * 
	 * @param article
	 *            源文章
	 * @param dquery
	 *            文章详情查询接口
	 * @param limit
	 *            文章数
	 * @return
	 */
	public List<Article> querySimilar(Article article, ArticlesDetailQuery dquery, int limit) {
		IndexSearcher searcher = null;
		try {
			searcherManager.maybeRefresh();
			searcher = searcherManager.acquire();

			Query likeQuery = buildLikeQuery(article, searcher);
			if (likeQuery == null) {
				return Collections.emptyList();
			}
			Builder builder = new Builder();
			builder.add(likeQuery, Occur.MUST);
			builder.add(new TermQuery(new Term(SPACE_ID, article.getSpace().getId().toString())), Occur.MUST);
			TopDocs likeDocs = searcher.search(builder.build(), limit + 1);
			List<Integer> datas = Lists.newArrayList();
			for (ScoreDoc scoreDoc : likeDocs.scoreDocs) {
				Document aSimilar = searcher.doc(scoreDoc.doc);
				datas.add(Integer.parseInt(aSimilar.get(ID)));
			}
			if (datas.isEmpty()) {
				return Lists.newArrayList();
			}
			List<Article> articles = dquery.query(datas);
			if (!articles.isEmpty()) {
				articles.sort(COMPARATOR);
			}
			List<Article> results = articles.stream().filter(art -> !art.equals(article)).collect(Collectors.toList());
			int size = results.size();
			int max = Math.min(limit, size);
			return size > max ? results.subList(0, max) : results;
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		} finally {
			try {
				if (searcher != null) {
					searcherManager.release(searcher);
				}
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
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

	/**
	 * 查询文章
	 * 
	 * @param param
	 *            查询参数
	 * @param dquery
	 *            文章详情接口
	 * @return 分页内容
	 */
	public PageResult<Article> query(ArticleQueryParam param, ArticlesDetailQuery dquery) {
		IndexSearcher searcher = null;
		try {
			searcherManager.maybeRefresh();
			searcher = searcherManager.acquire();
			Sort sort = buildSort(param);

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
			ArticleFrom from = param.getFrom();
			if (from != null) {
				Query query = new TermQuery(new Term(FROM, from.name().toLowerCase()));
				builder.add(query, Occur.MUST);
			}
			if (param.getTag() != null) {
				builder.add(new TermQuery(new Term(TAG, param.getTag())), Occur.MUST);
			}
			Optional<Query> optionalMultiFieldQuery = param.hasQuery() ? buildMultiFieldQuery(param.getQuery())
					: Optional.empty();
			optionalMultiFieldQuery.ifPresent(query -> builder.add(query, Occur.MUST));
			Query query = builder.build();
			LOGGER.debug(query.toString());

			TopDocs tds = searcher.search(query, MAX_RESULTS, sort);
			int total = tds.totalHits;
			int offset = param.getOffset();
			Map<Integer, Document> datas = Maps.newLinkedHashMap();
			if (offset < total) {
				ScoreDoc[] docs = tds.scoreDocs;
				int last = offset + param.getPageSize();
				for (int i = offset; i < Math.min(Math.min(last, total), MAX_RESULTS); i++) {
					int docId = docs[i].doc;
					Document doc = searcher.doc(docId);
					datas.put(Integer.parseInt(doc.get(ID)), doc);
				}
			}
			List<Article> articles = dquery.query(Lists.newArrayList(datas.keySet()));
			if (param.isHighlight() && optionalMultiFieldQuery.isPresent()) {
				for (Article article : articles) {
					doHightlight(article, datas.get(article.getId()), optionalMultiFieldQuery.get());
					article.setContent(null);
				}
			}
			return new PageResult<>(param, Math.min(MAX_RESULTS, total), articles);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		} finally {
			try {
				if (searcher != null) {
					searcherManager.release(searcher);
				}
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	protected Optional<Query> buildMultiFieldQuery(String query) {
		String escaped = MultiFieldQueryParser.escape(query.trim());
		MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[] { TAG, TITLE, ALIAS, SUMMARY, CONTENT },
				analyzer, qboostMap);
		parser.setAutoGeneratePhraseQueries(true);
		parser.setPhraseSlop(0);
		try {
			return Optional.of(parser.parse(escaped));
		} catch (ParseException e) {
			LOGGER.debug("无法解析输入的查询表达式:" + escaped + ":" + e.getMessage(), e);
			return Optional.empty();
		}
	}

	protected Sort buildSort(ArticleQueryParam param) {
		List<SortField> fields = Lists.newArrayList();
		if (!param.isIgnoreLevel()) {
			fields.add(new SortField(LEVEL, Type.INT, true));
		}
		ArticleQueryParam.Sort psort = param.getSort();
		if (psort == null) {
			fields.add(SortField.FIELD_SCORE);
		} else {
			switch (param.getSort()) {
			case HITS:
				fields.add(new SortField(HITS, Type.INT, true));
				break;
			case PUBDATE:
				fields.add(new SortField(PUB_DATE, SortField.Type.STRING, true));
				break;
			case LASTMODIFYDATE:
				fields.add(new SortField(LASTMODIFYDATE, SortField.Type.STRING, true));
				fields.add(new SortField(PUB_DATE, SortField.Type.STRING, true));
			default:
				break;
			}
			fields.add(new SortField(ID, SortField.Type.STRING, true));
		}
		return new Sort(fields.toArray(new SortField[] {}));
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
	protected void doHightlight(Article article, Document doc, Query query) {

		String content = doc.get(CONTENT);
		String summary = doc.get(SUMMARY);
		String title = doc.get(TITLE);
		String[] tags = doc.getValues(TAG);

		getHightlight(new Highlighter(titleFormatter, new QueryScorer(query)), TITLE, title)
				.ifPresent(hl -> article.setTitle(hl));
		Optional<String> summaryHl = getHightlight(new Highlighter(summaryFormatter, new QueryScorer(query)), SUMMARY,
				summary);
		if (summaryHl.isPresent()) {
			article.setSummary(summaryHl.get());
		} else {
			getHightlight(new Highlighter(summaryFormatter, new QueryScorer(query)), CONTENT, content)
					.ifPresent(hl -> article.setSummary(hl));
		}
		if (tags != null && tags.length > 0) {
			for (String tag : tags) {
				Optional<Tag> optionalTag = article.getTag(tag);
				if (optionalTag.isPresent()) {
					Tag _tag = optionalTag.get();
					getHightlight(new Highlighter(tagFormatter, new QueryScorer(query)), TAG, _tag.getName())
							.ifPresent(hl -> _tag.setName(hl));
				}
			}
		}

	}

	private Optional<String> getHightlight(Highlighter highlighter, String fieldName, String text) {
		try {
			return Optional.ofNullable(highlighter.getBestFragment(analyzer, fieldName, text));
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	/**
	 * 删除所有已经存在的文章索引
	 */
	public void deleteAll() {
		try {
			oriWriter.deleteAll();
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	private String clean(String content) {
		// 只需要纯文字的内容
		return Jsoup.clean(content, Whitelist.none());
	}

	private String cleanContent(Article article) {
		if (articleContentHandler != null) {
			articleContentHandler.handle(article);
		}
		return clean(article.getContent());
	}

	/**
	 * 删除标签
	 * 
	 * @param tags
	 *            要删除的标签名
	 */
	public abstract void removeTag(String... tags);

	/**
	 * 增加标签
	 * 
	 * @param tags
	 *            标签名
	 */
	public abstract void addTags(String... tags);

	@Override
	public void afterPropertiesSet() throws Exception {
		if (titleFormatter == null) {
			titleFormatter = new DefaultFormatter("lucene-highlight-title");
		}
		if (tagFormatter == null) {
			tagFormatter = new DefaultFormatter("lucene-highlight-tag");
		}
		if (summaryFormatter == null) {
			summaryFormatter = new DefaultFormatter("lucene-highlight-summary");
		}
		qboostMap.put(TAG, boostMap.getOrDefault(TAG, 20F));
		qboostMap.put(ALIAS, boostMap.getOrDefault(ALIAS, 10F));
		qboostMap.put(TITLE, boostMap.getOrDefault(TITLE, 7F));
		qboostMap.put(SUMMARY, boostMap.getOrDefault(SUMMARY, 3F));
		qboostMap.put(CONTENT, boostMap.getOrDefault(CONTENT, 1F));
		if (commitPeriod <= 0) {
			commitPeriod = DEFAULT_COMMIT_PERIOD;
		}
		threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
			try {
				oriWriter.commit();
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
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

		private DefaultFormatter(String classes) {
			this.classes = classes;
		}

		@Override
		public String highlightTerm(String originalText, TokenGroup tokenGroup) {
			return tokenGroup.getTotalScore() <= 0 ? originalText
					: new StringBuilder("<b class=\"").append(classes).append("\">").append(originalText).append("</b>")
							.toString();
		}
	}

	/**
	 * 文章查询接口
	 * 
	 * @author Administrator
	 *
	 */
	@FunctionalInterface
	public interface ArticlesDetailQuery {
		/**
		 * 通过id集合查询对应的文章
		 * 
		 * @param ids
		 *            要查询的文章id集合
		 * @return 文章集合
		 */
		List<Article> query(List<Integer> ids);
	}

	public void setBoostMap(Map<String, Float> boostMap) {
		this.boostMap = boostMap;
	}

	public void setCommitPeriod(int commitPeriod) {
		this.commitPeriod = commitPeriod;
	}

}
