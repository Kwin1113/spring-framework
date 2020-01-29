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

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheOperationSource;

/**
 * AspectJ缓存切面的抽象父类切面。具体的子类切面要通过例如Java 5 注解的策略实现
 * {@link #cacheMethodExecution}切入点
 *
 * 不论在Spring IoC容器内外都适用。合理地设置{@link #setCacheManager cacheManager}
 * 属性，就能够适用Spring支持的所有缓存实现方案。
 *
 * 注意：如果一个方法实现了一个接口，该接口本身就是缓存注解的，则不会解析相关的Spring缓存定义。
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @since 3.1
 */
public abstract aspect AbstractCacheAspect extends CacheAspectSupport implements DisposableBean {

	protected AbstractCacheAspect() {
	}

	/**
	 * 通过给定的缓存元数据检索策略构造对象
	 * @param cos {@link CacheOperationSource}实现, 在每个切入点检索Spring cache缓存元数据
	 */
	protected AbstractCacheAspect(CacheOperationSource... cos) {
		setCacheOperationSources(cos);
	}

	@Override
	public void destroy() {
		clearMetadataCache(); // An aspect is basically a singleton
	}

	@SuppressAjWarnings("adviceDidNotMatch")
	Object around(final Object cachedObject) : cacheMethodExecution(cachedObject) {
		//获取切入点的实际方法
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
	 * 具体的子切面需要实现该切入点，指定缓存方法
	 */
	protected abstract pointcut cacheMethodExecution(Object cachedObject);

}
