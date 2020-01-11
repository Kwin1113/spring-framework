/*
 * Copyright 2002-2017 the original author or authors.
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

/**
 * 处理缓存相关错误的策略。在大多数情况下，提供者抛出的所有异常都应该被简单地直接
 * 抛回客户端，但是在一些情况下，基础结构需要用特殊的方式去处理缓存提供者的异常。
 *
 * 通常情况下，通过给定的id在缓存中未找到对象的时候，可以被直接处理为缓存未命中，
 * 而不是抛出异常。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface CacheErrorHandler {

	/**
	 * 处理在检索给定{@code key}时缓存提供者抛出的运行时异常，也有可能被
	 * 以致命异常的形式重新抛出。
	 * @param exception 缓存提供者抛出的异常
	 * @param cache 缓存
	 * @param key 检索的key
	 * @see Cache#get(Object)
	 */
	void handleCacheGetError(RuntimeException exception, Cache cache, Object key);

	/**
	 * 处理在更新给定{@code key}和@{@code value}时缓存提供者抛出的运行时异常，也有可能被
	 * 以致命异常的形式重新抛出。
	 * @param exception 缓存提供者抛出的异常
	 * @param cache 缓存
	 * @param key 更新的key
	 * @param value 更新的value
	 * @see Cache#put(Object, Object)
	 */
	void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value);

	/**
	 * 处理在移除给定{@code key}时缓存提供者抛出的运行时异常，也有可能被
	 * 以致命异常的形式重新抛出。
	 * @param exception 缓存提供者抛出的异常
	 * @param cache 缓存
	 * @param key 移除的key
	 */
	void handleCacheEvictError(RuntimeException exception, Cache cache, Object key);

	/**
	 * 处理在清空特定{@link Cache}时缓存提供者抛出的运行时异常，也有可能被
	 * 以致命异常的形式重新抛出。
	 * @param exception 缓存提供者抛出的异常
	 * @param cache 清空的缓存
	 */
	void handleCacheClearError(RuntimeException exception, Cache cache);

}
