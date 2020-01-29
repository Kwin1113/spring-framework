/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.cache.aspectj;

import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

/**
 * Spring @{@link Cacheable}注解的具体AspectJ缓存切面
 *
 * <p>当使用该切面时，必须注解在实现类（或该类的方法上），而不能注解在该类所实现的接口上
 * AspectJ遵守Java规范，接口上的注解是不能被继承的AspectJ follows Java's rule that annotations on interfaces are <i>not</i>
 * inherited.
 *
 * 类上的{@code @Cacheable}注解以默认缓存语义指定该类中的所有public操作
 *
 * 类中方法上的{@code @Cacheable}注解将覆盖类上默认缓存语义（如果存在的话）。任何方法都可能被
 * 注解（不论是否可见）。直接注解非公共方法是获得用于执行此类操作的缓存分界的唯一方法。
 *
 * @author Costin Leau
 * @since 3.1
 */
public aspect AnnotationCacheAspect extends AbstractCacheAspect {

	public AnnotationCacheAspect() {
		super(new AnnotationCacheOperationSource(false));
	}

	/**
	 * 匹配任何有@{@link Cacheable}注解的类或有@{@link Cacheable}注解的子类的方法执行。
	 */
	private pointcut executionOfAnyPublicMethodInAtCacheableType() :
		execution(public * ((@Cacheable *)+).*(..)) && within(@Cacheable *);

	/**
	 * 匹配任何有@{@link CacheEvict}注解的类或有@{@link CacheEvict}注解的子类的方法执行。
	 */
	private pointcut executionOfAnyPublicMethodInAtCacheEvictType() :
		execution(public * ((@CacheEvict *)+).*(..)) && within(@CacheEvict *);

	/**
	 * 匹配任何有@{@link CachePut}注解的类或有@{@link CachePut}注解的子类的方法执行。
	 */
	private pointcut executionOfAnyPublicMethodInAtCachePutType() :
		execution(public * ((@CachePut *)+).*(..)) && within(@CachePut *);

	/**
	 * 匹配任何有@{@link Caching}注解的类或有@{@link Caching}注解的子类的方法执行。
	 */
	private pointcut executionOfAnyPublicMethodInAtCachingType() :
		execution(public * ((@Caching *)+).*(..)) && within(@Caching *);

	/**
	 * 匹配@{@link Cacheable}注解的所有方法执行
	 * Matches the execution of any method with the @{@link Cacheable} annotation.
	 */
	private pointcut executionOfCacheableMethod() :
		execution(@Cacheable * *(..));

	/**
	 * 匹配@{@link CacheEvict}注解的所有方法执行
	 */
	private pointcut executionOfCacheEvictMethod() :
		execution(@CacheEvict * *(..));

	/**
	 * 匹配@{@link CachePut}注解的所有方法执行
	 */
	private pointcut executionOfCachePutMethod() :
		execution(@CachePut * *(..));

	/**
	 * 匹配@{@link Caching}注解的所有方法执行
	 */
	private pointcut executionOfCachingMethod() :
		execution(@Caching * *(..));

	/**
	 * 定义父类切面的切入点 - 匹配的切入点将应用Spring缓存管理。
	 */
	protected pointcut cacheMethodExecution(Object cachedObject) :
		(executionOfAnyPublicMethodInAtCacheableType()
				|| executionOfAnyPublicMethodInAtCacheEvictType()
				|| executionOfAnyPublicMethodInAtCachePutType()
				|| executionOfAnyPublicMethodInAtCachingType()
				|| executionOfCacheableMethod()
				|| executionOfCacheEvictMethod()
				|| executionOfCachePutMethod()
				|| executionOfCachingMethod())
			&& this(cachedObject);
}