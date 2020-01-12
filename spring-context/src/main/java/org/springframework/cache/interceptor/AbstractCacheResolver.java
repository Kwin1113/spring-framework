/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.interceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 一个{@link CacheResolver}的基础实现类，要求具体的实现类提供基于调用
 * 上下文的缓存名称集合。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 */
public abstract class AbstractCacheResolver implements CacheResolver, InitializingBean {

	/**
	 * 依赖于CacheManager
	 */
	@Nullable
	private CacheManager cacheManager;


	/**
	 * 创建一个新的{@code AbstractCacheResolver}。
	 *
	 * @see #setCacheManager
	 */
	protected AbstractCacheResolver() {
	}

	/**
	 * 通过给定的{@link CacheManager}创建一个新的{@code AbstractCacheResolver}。
	 *
	 * @param cacheManager 使用的 CacheManager
	 */
	protected AbstractCacheResolver(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}


	/**
	 * 设置该实例要使用的{@link CacheManager}
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * 返回该实例使用的{@link CacheManager}
	 */
	public CacheManager getCacheManager() {
		Assert.state(this.cacheManager != null, "No CacheManager set");
		return this.cacheManager;
	}

	/**
	 * 验证CacheManager必须存在
	 */
	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.cacheManager, "CacheManager is required");
	}


	@Override
	public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
		Collection<String> cacheNames = getCacheNames(context);
		if (cacheNames == null) {
			return Collections.emptyList();
		}
		Collection<Cache> result = new ArrayList<>(cacheNames.size());
		//根据缓存名称从缓存管理器CacheManager中获取缓存
		for (String cacheName : cacheNames) {
			Cache cache = getCacheManager().getCache(cacheName);
			if (cache == null) {
				throw new IllegalArgumentException("Cannot find cache named '" +
						cacheName + "' for " + context.getOperation());
			}
			result.add(cache);
		}
		return result;
	}

	/**
	 * 根据当前的缓存管理器提供需要解析的缓存名称。
	 * 该会返回{@code null}，表示这次调用没有缓存可以解析。
	 *
	 * @param context 特定调用的上下文
	 * @return 需要解析的缓存名称集合，如果没有缓存可以解析则返回{@code null}
	 */
	@Nullable
	protected abstract Collection<String> getCacheNames(CacheOperationInvocationContext<?> context);

}
