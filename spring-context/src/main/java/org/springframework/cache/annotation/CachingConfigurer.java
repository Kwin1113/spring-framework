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

package org.springframework.cache.annotation;

import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.lang.Nullable;

/**
 * @ {@link EnableCaching}注解的配置类@{@link org.springframework.context.annotation.Configuration
 * Configuration}的实现接口，该注解明确特定地指定在注解驱动的缓存管理中，如何处理缓存和key的生成方法。
 * 子类{@link CachingConfigurerSupport}实现了该接口的所有基础实现。
 *
 * <p>See @{@link EnableCaching} for general examples and context; see
 * {@link #cacheManager()}, {@link #cacheResolver()} and {@link #keyGenerator()}
 * for detailed instructions.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 * @see EnableCaching
 * @see CachingConfigurerSupport
 */
public interface CachingConfigurer {

	/**
	 * 返回注解驱动的缓存管理下的缓存管理器 cache manager。
	 * 默认的 {@link CacheResolver} 将使用该缓存管理器在后台初始化。
	 * 如果要更细粒度的控制缓存方案，考虑直接设置 {@link CacheResolver}。
	 *
	 * 实现类必须显式声明。
	 * {@link org.springframework.context.annotation.Bean @Bean}, e.g.
	 * <pre class="code">
	 * @ Configuration
	 * @ EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     @ Bean // important!
	 *     @ Override
	 *     public CacheManager cacheManager() {
	 *         // configure and return CacheManager instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * See @{@link EnableCaching} for more complete examples.
	 */
	@Nullable
	CacheManager cacheManager();

	/**
	 * 返回在注解驱动的缓存管理下用于处理通用缓存的缓存处理器 {@link CacheResolver}。
	 * 这是一个比指定 {@link CacheManager} 更有用的另一种配置方式。
	 * 如果 {@link #cacheManager()} 和 {@code #cacheResolver()} 都设置了，缓存管理器将会被忽略，
	 * 优先使用缓存处理器。
	 *
	 * 实现类必须显式声明。
	 * {@link org.springframework.context.annotation.Bean @Bean}, e.g.
	 * <pre class="code">
	 * @ Configuration
	 * @ EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     @ Bean // important!
	 *     @ Override
	 *     public CacheResolver cacheResolver() {
	 *         // configure and return CacheResolver instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * See {@link EnableCaching} for more complete examples.
	 */
	@Nullable
	CacheResolver cacheResolver();

	/**
	 * 返回在注解驱动的缓存管理下的key生成器。
	 *
	 * 实现类必须显式声明。
	 * {@link org.springframework.context.annotation.Bean @Bean}, e.g.
	 * <pre class="code">
	 * @ Configuration
	 * @ EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     @ Bean // important!
	 *     @ Override
	 *     public KeyGenerator keyGenerator() {
	 *         // configure and return KeyGenerator instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * See @{@link EnableCaching} for more complete examples.
	 */
	@Nullable
	KeyGenerator keyGenerator();

	/**
	 * 返回处理缓存相关错误的 {@link CacheErrorHandler}。
	 * 默认情况下，使用{@link org.springframework.cache.interceptor.SimpleCacheErrorHandler}，
	 * 该类直接把异常抛出给客户端。
	 *
	 * 实现类必须显式声明。
	 * {@link org.springframework.context.annotation.Bean @Bean}, e.g.
	 * <pre class="code">
	 * @ Configuration
	 * @ EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     @ Bean // important!
	 *     @ Override
	 *     public CacheErrorHandler errorHandler() {
	 *         // configure and return CacheErrorHandler instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * See @{@link EnableCaching} for more complete examples.
	 */
	@Nullable
	CacheErrorHandler errorHandler();

}
