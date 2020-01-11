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
 * <p>
 * 该类方便划分声明式缓存划分：按名称，用缓存代理包装一个单例目标对象，代理目标实现类实现的所有接口。
 * 主要存在于集合第三方框架中。
 * <p>
 * 用户最好使用{@code cache:}XML命名空间、
 * {@link org.springframework.cache.annotation.Cacheable @Cacheable}注解。
 * <p>
 * See the
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#cache-annotations">declarative annotation-based caching</a>
 * section of the Spring reference documentation for more information.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see CacheInterceptor
 * @since 3.1
 */
@SuppressWarnings("serial")
public class CacheProxyFactoryBean extends AbstractSingletonProxyFactoryBean
		implements BeanFactoryAware, SmartInitializingSingleton {

	private final CacheInterceptor cacheInterceptor = new CacheInterceptor();

	private Pointcut pointcut = Pointcut.TRUE;


	/**
	 * 设置一个或多个源来查询缓存操作。
	 *
	 * @see CacheInterceptor#setCacheOperationSources
	 */
	public void setCacheOperationSources(CacheOperationSource... cacheOperationSources) {
		this.cacheInterceptor.setCacheOperationSources(cacheOperationSources);
	}

	/**
	 * 如果该操作没有指定特定的key生成器，指定该缓存切面需要委托的默认的{@link KeyGenerator}。
	 * 默认为{@link SimpleKeyGenerator}.
	 *
	 * @see CacheInterceptor#setKeyGenerator
	 * @since 5.0.3
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.cacheInterceptor.setKeyGenerator(keyGenerator);
	}

	/**
	 * 如果该操作没有指定特定的缓存处理器，指定该缓存切面需要委托的默认的{@link CacheResolver}。
	 * 默认的处理器根据默认的缓存处理器和缓存名称处理缓存。
	 *
	 * @see CacheInterceptor#setCacheResolver
	 * @since 5.0.3
	 */
	public void setCacheResolver(CacheResolver cacheResolver) {
		this.cacheInterceptor.setCacheResolver(cacheResolver);
	}

	/**
	 * 设置{@link CacheManager}来创建默认的{@link CacheResolver}。
	 * 如果存在{@link CacheResolver}，则替代他。
	 *
	 * @see CacheInterceptor#setCacheManager
	 * @since 5.0.3
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheInterceptor.setCacheManager(cacheManager);
	}

	/**
	 * 设置一个切面，即一个触发{@link CacheInterceptor}的条件性调用conditional invocation的bean。
	 * 该缓存拦截器依赖于方法和传参的
	 * 注意：始终调用其他拦截器。
	 * <p>Note: Additional interceptors are always invoked.
	 *
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
