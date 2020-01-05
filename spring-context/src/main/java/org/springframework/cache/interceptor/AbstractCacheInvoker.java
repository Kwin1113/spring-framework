/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;
import org.springframework.util.function.SingletonSupplier;

/**
 * 调用缓存操作{@link Cache} operations的基础组件，发生异常时会使用
 * 配置的{@link CacheErrorHandler}处理。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @see org.springframework.cache.interceptor.CacheErrorHandler
 * @since 4.1
 */
public abstract class AbstractCacheInvoker {

	protected SingletonSupplier<CacheErrorHandler> errorHandler;


	protected AbstractCacheInvoker() {
		this.errorHandler = SingletonSupplier.of(SimpleCacheErrorHandler::new);
	}

	protected AbstractCacheInvoker(CacheErrorHandler errorHandler) {
		this.errorHandler = SingletonSupplier.of(errorHandler);
	}


	/**
	 * 把参数中的{@link CacheErrorHandler}用于处理缓存提供者抛出的异常。
	 * 默认情况下使用{@link SimpleCacheErrorHandler}，该异常处理器将
	 * 不处理异常，原模原样抛出。
	 */
	public void setErrorHandler(CacheErrorHandler errorHandler) {
		this.errorHandler = SingletonSupplier.of(errorHandler);
	}

	/**
	 * 返回使用的{@link CacheErrorHandler}。
	 */
	public CacheErrorHandler getErrorHandler() {
		return this.errorHandler.obtain();
	}


	/**
	 * 在指定的{@link Cache}上执行{@link Cache#get(Object)}操作，并且在发生
	 * 异常时调用异常处理器。
	 * 在发生错误的情况下模拟缓存未命中。
	 *
	 * @see Cache#get(Object)
	 */
	@Nullable
	protected Cache.ValueWrapper doGet(Cache cache, Object key) {
		try {
			return cache.get(key);
		} catch (RuntimeException ex) {
			getErrorHandler().handleCacheGetError(ex, cache, key);
			return null;  // If the exception is handled, return a cache miss
		}
	}

	/**
	 * 在指定的{@link Cache}上执行{@link Cache#put(Object, Object)}操作，并且在发生
	 * 异常时调用异常处理器。
	 */
	protected void doPut(Cache cache, Object key, @Nullable Object result) {
		try {
			cache.put(key, result);
		} catch (RuntimeException ex) {
			getErrorHandler().handleCachePutError(ex, cache, key, result);
		}
	}

	/**
	 * 在指定的{@link Cache}上执行{@link Cache#evict(Object)}/{@link Cache#evictIfPresent(Object)}
	 * 操作，并且在发生异常时调用异常处理器。
	 */
	protected void doEvict(Cache cache, Object key, boolean immediate) {
		try {
			if (immediate) {
				cache.evictIfPresent(key);
			} else {
				cache.evict(key);
			}
		} catch (RuntimeException ex) {
			getErrorHandler().handleCacheEvictError(ex, cache, key);
		}
	}

	/**
	 * 在指定的{@link Cache}上执行{@link Cache#clear()}操作，并且在发生
	 * 异常时调用异常处理器。
	 */
	protected void doClear(Cache cache, boolean immediate) {
		try {
			if (immediate) {
				cache.invalidate();
			} else {
				cache.clear();
			}
		} catch (RuntimeException ex) {
			getErrorHandler().handleCacheClearError(ex, cache);
		}
	}

}
