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
package me.qyh.blog.ui;

import java.util.HashSet;
import java.util.Set;

import org.thymeleaf.cache.AbstractCacheManager;
import org.thymeleaf.cache.ExpressionCacheKey;
import org.thymeleaf.cache.ICache;
import org.thymeleaf.cache.ICacheEntryValidityChecker;
import org.thymeleaf.cache.TemplateCacheKey;
import org.thymeleaf.engine.TemplateManager;
import org.thymeleaf.engine.TemplateModel;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class UICacheManager extends AbstractCacheManager {

	private static final ICache<TemplateCacheKey, TemplateModel> templateCache = new UICache<>(
			Caffeine.newBuilder().build());
	private static final ICache<ExpressionCacheKey, Object> expressionCache = new UICache<>(
			Caffeine.newBuilder().build());

	@Override
	protected ICache<TemplateCacheKey, TemplateModel> initializeTemplateCache() {
		return templateCache;
	}

	@Override
	protected ICache<ExpressionCacheKey, Object> initializeExpressionCache() {
		return expressionCache;
	}

	public static void clearAll() {
		templateCache.clear();
	}

	/**
	 * @see TemplateManager#clearCachesFor(String)
	 * @param template
	 */
	public static void clearCachesFor(final String template) {
		final Set<TemplateCacheKey> keysToBeRemoved = new HashSet<>(4);
		final Set<TemplateCacheKey> templateCacheKeys = templateCache.keySet();
		for (final TemplateCacheKey templateCacheKey : templateCacheKeys) {
			final String ownerTemplate = templateCacheKey.getOwnerTemplate();
			if (ownerTemplate != null) {
				if (ownerTemplate.equals(template)) {
					keysToBeRemoved.add(templateCacheKey);
				}
			} else {
				if (templateCacheKey.getTemplate().equals(template)) {
					keysToBeRemoved.add(templateCacheKey);
				}
			}
		}
		for (final TemplateCacheKey keyToBeRemoved : keysToBeRemoved) {
			templateCache.clearKey(keyToBeRemoved);
		}
	}

	private static final class UICache<K, V> implements ICache<K, V> {

		private final Cache<K, V> cache;

		public UICache(Cache<K, V> cache) {
			super();
			this.cache = cache;
		}

		@Override
		public void put(K key, V value) {
			cache.put(key, value);
		}

		@Override
		public V get(K key) {
			if (ParseContext.isDisposible()) {
				return null;
			}
			return cache.getIfPresent(key);
		}

		@Override
		public V get(K key, ICacheEntryValidityChecker<? super K, ? super V> validityChecker) {
			return get(key);
		}

		@Override
		public void clear() {
			cache.invalidateAll();
		}

		@Override
		public void clearKey(K key) {
			cache.invalidate(key);
		}

		@Override
		public Set<K> keySet() {
			return cache.asMap().keySet();
		}
	}
}
