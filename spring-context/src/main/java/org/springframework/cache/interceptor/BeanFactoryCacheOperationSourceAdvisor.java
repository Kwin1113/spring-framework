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

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;
import org.springframework.lang.Nullable;

/**
 * {@link CacheOperationSource}驱动的增强，用于为可缓存的方法创建一个缓存增强。
 *
 * @author Costin Leau
 * @since 3.1
 */
@SuppressWarnings("serial")
public class BeanFactoryCacheOperationSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

	@Nullable
	private CacheOperationSource cacheOperationSource;

	private final CacheOperationSourcePointcut pointcut = new CacheOperationSourcePointcut() {
		@Override
		@Nullable
		protected CacheOperationSource getCacheOperationSource() {
			return cacheOperationSource;
		}
	};


	/**
	 * 设置缓存属性源，用于查询缓存属性。通常应与缓存拦截器本身设置的源相同。
	 */
	public void setCacheOperationSource(CacheOperationSource cacheOperationSource) {
		this.cacheOperationSource = cacheOperationSource;
	}

	/**
	 * 设置切入点使用的类过滤器{@link ClassFilter}。
	 * 默认是{@link ClassFilter#TRUE}。
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.pointcut.setClassFilter(classFilter);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
