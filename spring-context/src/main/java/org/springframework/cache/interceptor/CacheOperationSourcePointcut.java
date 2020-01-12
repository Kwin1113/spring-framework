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

import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * 如果底层{@link CacheOperationSource}具有给定方法的属性，则匹配该Pointcut。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
@SuppressWarnings("serial")
abstract class CacheOperationSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

	protected CacheOperationSourcePointcut() {
		setClassFilter(new CacheOperationSourceClassFilter());
	}


	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		CacheOperationSource cas = getCacheOperationSource();
		return (cas != null && !CollectionUtils.isEmpty(cas.getCacheOperations(method, targetClass)));
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CacheOperationSourcePointcut)) {
			return false;
		}
		CacheOperationSourcePointcut otherPc = (CacheOperationSourcePointcut) other;
		return ObjectUtils.nullSafeEquals(getCacheOperationSource(), otherPc.getCacheOperationSource());
	}

	@Override
	public int hashCode() {
		return CacheOperationSourcePointcut.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + getCacheOperationSource();
	}


	/**
	 * 获得底层的{@link CacheOperationSource}（可能为{@code null}）
	 * 需要被子类实现。
	 */
	@Nullable
	protected abstract CacheOperationSource getCacheOperationSource();


	/**
	 * 委托给{@link CacheOperationSource#isCandidateClass}的类过滤器{@link ClassFilter}，
	 * 用来过滤不用搜索的类。
	 */
	private class CacheOperationSourceClassFilter implements ClassFilter {

		@Override
		public boolean matches(Class<?> clazz) {
			//如果切入点的类是个CacheManager，不切入
			if (CacheManager.class.isAssignableFrom(clazz)) {
				return false;
			}
			//获取当前的缓存属性源
			CacheOperationSource cas = getCacheOperationSource();
			return (cas == null || cas.isCandidateClass(clazz));
		}
	}

}
