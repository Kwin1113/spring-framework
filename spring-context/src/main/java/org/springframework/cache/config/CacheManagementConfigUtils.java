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

package org.springframework.cache.config;

/**
 * 用于子包内部用于共享的配置常量。
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
public abstract class CacheManagementConfigUtils {

	/**
	 * 缓存增强bean的名称
	 */
	public static final String CACHE_ADVISOR_BEAN_NAME =
			"org.springframework.cache.config.internalCacheAdvisor";

	/**
	 * 缓存切面bean的名称
	 */
	public static final String CACHE_ASPECT_BEAN_NAME =
			"org.springframework.cache.config.internalCacheAspect";

	/**
	 * JCache增强bean的名称
	 */
	public static final String JCACHE_ADVISOR_BEAN_NAME =
			"org.springframework.cache.config.internalJCacheAdvisor";

	/**
	 * JCache切面bean的名称
	 */
	public static final String JCACHE_ASPECT_BEAN_NAME =
			"org.springframework.cache.config.internalJCacheAspect";

}
