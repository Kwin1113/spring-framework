/*
 * Copyright 2002-2014 the original author or authors.
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

import java.lang.reflect.Method;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;

import org.aspectj.lang.annotation.RequiredTypes;
import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.jcache.interceptor.JCacheAspectSupport;

/**
 * 使用JSR-107标准注解的具体AspectJ缓存切面
 *
 * 使用该切面时，必须注解在实现类（和/或类中的方法）上，不能注解在接口上。AspectJ遵守
 * Java规范，接口上的注解不会被继承
 *
 * 任何方法都能被注解（不管可见性）。直接注解非public方法是获得此类操作的缓存划分的唯一方法
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@RequiredTypes({"org.springframework.cache.jcache.interceptor.JCacheAspectSupport", "javax.cache.annotation.CacheResult"})
public aspect JCacheCacheAspect extends JCacheAspectSupport {

	@SuppressAjWarnings("adviceDidNotMatch")
	Object around(final Object cachedObject) : cacheMethodExecution(cachedObject) {
		MethodSignature methodSignature = (MethodSignature) thisJoinPoint.getSignature();
		Method method = methodSignature.getMethod();

		CacheOperationInvoker aspectJInvoker = new CacheOperationInvoker() {
			public Object invoke() {
				try {
					return proceed(cachedObject);
				}
				catch (Throwable ex) {
					throw new ThrowableWrapper(ex);
				}
			}

		};

		try {
			return execute(aspectJInvoker, thisJoinPoint.getTarget(), method, thisJoinPoint.getArgs());
		}
		catch (CacheOperationInvoker.ThrowableWrapper th) {
			AnyThrow.throwUnchecked(th.getOriginal());
			return null; // never reached
		}
	}

	/**
	 * 定义切入点：匹配满足JSR-107规范的缓存管理器切入点
	*/
	protected pointcut cacheMethodExecution(Object cachedObject) :
			(executionOfCacheResultMethod()
				|| executionOfCachePutMethod()
				|| executionOfCacheRemoveMethod()
				|| executionOfCacheRemoveAllMethod())
			&& this(cachedObject);

	/**
	 * 匹配任何带有@{@link CacheResult}注解的方法执行
	 */
	private pointcut executionOfCacheResultMethod() :
		execution(@CacheResult * *(..));

	/**
	 * 匹配任何带有@{@link CachePut}注解的方法执行
	 */
	private pointcut executionOfCachePutMethod() :
		execution(@CachePut * *(..));

	/**
	 * 匹配任何带有@{@link CacheRemove}注解的方法执行
	 */
	private pointcut executionOfCacheRemoveMethod() :
		execution(@CacheRemove * *(..));

	/**
	 * 匹配任何带有@{@link CacheRemoveAll}注解的方法执行
	 */
	private pointcut executionOfCacheRemoveAllMethod() :
		execution(@CacheRemoveAll * *(..));


}
