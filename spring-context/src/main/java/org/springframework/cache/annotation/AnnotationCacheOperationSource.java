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

package org.springframework.cache.annotation;

import org.springframework.cache.interceptor.AbstractFallbackCacheOperationSource;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 以注解格式缓存元数据的{@link org.springframework.cache.interceptor.CacheOperationSource CacheOperationSource}
 * 接口实现类。
 * <p>
 * 该类读取Spring的{@link Cacheable}, {@link CachePut} and {@link CacheEvict}注解，并暴露Spring缓存基础下
 * 的相应缓存操作定义。该类也作为自定义 {@code CacheOperationSource} 基类。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
@SuppressWarnings("serial")
public class AnnotationCacheOperationSource extends AbstractFallbackCacheOperationSource implements Serializable {

	private final boolean publicMethodsOnly;

	private final Set<CacheAnnotationParser> annotationParsers;


	/**
	 * 默认构造器。
	 * 支持{@code Cacheable} 和 {@code CacheEvict}的public方法。
	 */
	public AnnotationCacheOperationSource() {
		this(true);
	}

	/**
	 * 默认构造器。
	 * 支持{@code Cacheable} 和 {@code CacheEvict}的public方法。
	 *
	 * @param publicMethodsOnly 是否只支持public方法
	 *                          通常用于基于代理的AOP，或protected/private方法。
	 *                          通常和 AspectJ class weaving 一起使用。
	 */
	public AnnotationCacheOperationSource(boolean publicMethodsOnly) {
		this.publicMethodsOnly = publicMethodsOnly;
		this.annotationParsers = Collections.singleton(new SpringCacheAnnotationParser());
	}

	/**
	 * 创建一个自定义的AnnotationCacheOperationSource。
	 *
	 * @param annotationParser 使用的CacheAnnotationParser
	 */
	public AnnotationCacheOperationSource(CacheAnnotationParser annotationParser) {
		this.publicMethodsOnly = true;
		Assert.notNull(annotationParser, "CacheAnnotationParser must not be null");
		this.annotationParsers = Collections.singleton(annotationParser);
	}

	/**
	 * 创建一个自定义的AnnotationCacheOperationSource。
	 *
	 * @param annotationParsers 使用的CacheAnnotationParser
	 */
	public AnnotationCacheOperationSource(CacheAnnotationParser... annotationParsers) {
		this.publicMethodsOnly = true;
		Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
		this.annotationParsers = new LinkedHashSet<>(Arrays.asList(annotationParsers));
	}

	/**
	 * 创建一个自定义的AnnotationCacheOperationSource。
	 *
	 * @param annotationParsers 使用的CacheAnnotationParser
	 */
	public AnnotationCacheOperationSource(Set<CacheAnnotationParser> annotationParsers) {
		this.publicMethodsOnly = true;
		Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
		this.annotationParsers = annotationParsers;
	}


	@Override
	public boolean isCandidateClass(Class<?> targetClass) {
		//如果任一解析器能够解析目标类，则可以
		for (CacheAnnotationParser parser : this.annotationParsers) {
			if (parser.isCandidateClass(targetClass)) {
				return true;
			}
		}
		return false;
	}

	@Override
	@Nullable
	protected Collection<CacheOperation> findCacheOperations(Class<?> clazz) {
		return determineCacheOperations(parser -> parser.parseCacheAnnotations(clazz));
	}

	@Override
	@Nullable
	protected Collection<CacheOperation> findCacheOperations(Method method) {
		return determineCacheOperations(parser -> parser.parseCacheAnnotations(method));
	}

	/**
	 * 通过给定的 {@link CacheOperationProvider} 确定缓存操作。
	 * 实现类委托给已配置的{@link CacheAnnotationParser CacheAnnotationParsers}，
	 * 来解析已知的注解获得Spring元数据属性类。
	 * 可以重写用于支持自定义注解以支持缓存元数据。
	 *
	 * @param provider 缓存操作提供者
	 * @return 已配置的缓存操作，如果未找到，则返回{@code null}。
	 */
	@Nullable
	protected Collection<CacheOperation> determineCacheOperations(CacheOperationProvider provider) {
		Collection<CacheOperation> ops = null;
		for (CacheAnnotationParser parser : this.annotationParsers) {
			Collection<CacheOperation> annOps = provider.getCacheOperations(parser);
			if (annOps != null) {
				if (ops == null) {
					ops = annOps;
				} else {
					Collection<CacheOperation> combined = new ArrayList<>(ops.size() + annOps.size());
					combined.addAll(ops);
					combined.addAll(annOps);
					ops = combined;
				}
			}
		}
		return ops;
	}

	/**
	 * 默认情况下，只有public方法能够缓存。
	 */
	@Override
	protected boolean allowPublicMethodsOnly() {
		return this.publicMethodsOnly;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AnnotationCacheOperationSource)) {
			return false;
		}
		AnnotationCacheOperationSource otherCos = (AnnotationCacheOperationSource) other;
		return (this.annotationParsers.equals(otherCos.annotationParsers) &&
				this.publicMethodsOnly == otherCos.publicMethodsOnly);
	}

	@Override
	public int hashCode() {
		return this.annotationParsers.hashCode();
	}


	/**
	 * 基于给定 {@link CacheAnnotationParser} 的回调接口，用于提供 {@link CacheOperation} 实例。
	 */
	@FunctionalInterface
	protected interface CacheOperationProvider {

		/**
		 * 返回特定解析器提供的 {@link CacheOperation} 。
		 *
		 * @param parser 解析器
		 * @return 缓存操作，如果未找到，则返回{@code null}。
		 */
		@Nullable
		Collection<CacheOperation> getCacheOperations(CacheAnnotationParser parser);
	}

}
