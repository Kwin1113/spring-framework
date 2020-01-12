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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 通过迭代给定的{@code CacheOperationSource}实例数组组合{@link CacheOperationSource}
 * 的实现类。
 * 组合模式。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
@SuppressWarnings("serial")
public class CompositeCacheOperationSource implements CacheOperationSource, Serializable {

	private final CacheOperationSource[] cacheOperationSources;


	/**
	 * 通过给定的缓存操作源创建一个新的缓存操作源CompositeCacheOperationSource
	 * @param cacheOperationSources 合并的缓存操作源实例
	 */
	public CompositeCacheOperationSource(CacheOperationSource... cacheOperationSources) {
		Assert.notEmpty(cacheOperationSources, "CacheOperationSource array must not be empty");
		this.cacheOperationSources = cacheOperationSources;
	}

	/**
	 * 返回该{@code CompositeCacheOperationSource}合并的{@code CacheOperationSource}
	 * 实例数组。
	 */
	public final CacheOperationSource[] getCacheOperationSources() {
		return this.cacheOperationSources;
	}


	@Override
	public boolean isCandidateClass(Class<?> targetClass) {
		for (CacheOperationSource source : this.cacheOperationSources) {
			if (source.isCandidateClass(targetClass)) {
				return true;
			}
		}
		return false;
	}

	@Override
	@Nullable
	public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
		Collection<CacheOperation> ops = null;
		for (CacheOperationSource source : this.cacheOperationSources) {
			Collection<CacheOperation> cacheOperations = source.getCacheOperations(method, targetClass);
			if (cacheOperations != null) {
				if (ops == null) {
					ops = new ArrayList<>();
				}
				ops.addAll(cacheOperations);
			}
		}
		return ops;
	}

}
