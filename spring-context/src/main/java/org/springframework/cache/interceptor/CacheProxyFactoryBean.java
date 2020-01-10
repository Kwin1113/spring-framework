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

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.AbstractSingletonProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cache.CacheManager;

/**
 * 代理工厂bean，用于简化声明式缓存处理。
 * 这是使用标准AOP{@link org.springframework.aop.framework.ProxyFactoryBean}
 * 的一个便捷替代方式，并且包含一个{@link CacheInterceptor}定义。
 *
 * 该类方便划分声明式缓存划分：按名称，用缓存代理包装一个单例目标对象，代理目标实现类实现的所有接口。
 * 主要存在于集合第三方框架中。
 *
 * 用户最好使用{@code cache:}XML命名空间、
 * {@link org.springframework.cache.annotation.Cacheable @Cacheable}注解。
 *
 * See the
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#cache-annotations">declarative annotation-based caching</a>
 * section of the Spring reference documentation for more information.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see CacheInterceptor
 */
@SuppressWarnings("serial")
public class CacheProxyFactoryBean extends AbstractSingletonProxyFactoryBean
		implements BeanFactoryAware, SmartInitializingSingleton {

	private final CacheInterceptor cacheInterceptor = new CacheInterceptor();

	private Pointcut pointcut = Pointcut.TRUE;


	/**
	 * Set one or more sources to find cache operations.
	 * @see CacheInterceptor#setCacheOperationSources
	 */
	public void setCacheOperationSources(CacheOperationSource... cacheOperationSources) {
		this.cacheInterceptor.setCacheOperationSources(cacheOperationSources);
	}

	/**
	 * Set the default {@link KeyGenerator} that this cache aspect should delegate to
	 * if no specific key generator has been set for the operation.
	 * <p>The default is a {@link SimpleKeyGenerator}.
	 * @since 5.0.3
	 * @see CacheInterceptor#setKeyGenerator
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.cacheInterceptor.setKeyGenerator(keyGenerator);
	}

	/**
	 * Set the default {@link CacheResolver} that this cache aspect should delegate
	 * to if no specific cache resolver has been set for the operation.
	 * <p>The default resolver resolves the caches against their names and the
	 * default cache manager.
	 * @since 5.0.3
	 * @see CacheInterceptor#setCacheResolver
	 */
	public void setCacheResolver(CacheResolver cacheResolver) {
		this.cacheInterceptor.setCacheResolver(cacheResolver);
	}

	/**
	 * Set the {@link CacheManager} to use to create a default {@link CacheResolver}.
	 * Replace the current {@link CacheResolver}, if any.
	 * @since 5.0.3
	 * @see CacheInterceptor#setCacheManager
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheInterceptor.setCacheManager(cacheManager);
	}

	/**
	 * Set a pointcut, i.e. a bean that triggers conditional invocation of the
	 * {@link CacheInterceptor} depending on the method and attributes passed.
	 * <p>Note: Additional interceptors are always invoked.
	 * @see #setPreInterceptors
	 * @see #setPostInterceptors
	 */
	public void setPointcut(Pointcut pointcut) {
		this.pointcut = pointcut;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.cacheInterceptor.setBeanFactory(beanFactory);
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.cacheInterceptor.afterSingletonsInstantiated();
	}


	@Override
	protected Object createMainInterceptor() {
		this.cacheInterceptor.afterPropertiesSet();
		return new DefaultPointcutAdvisor(this.pointcut, this.cacheInterceptor);
	}

}
