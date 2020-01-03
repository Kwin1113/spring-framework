/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.annotation.*;

/**
 * {@code @CacheConfig} 提供了一种在类级别共享常见的缓存相关设置的机制。
 * <p>
 * 当此注解出现在给定的类上时，它将为该类中定义的任何缓存操作提供一组默认设置。
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheConfig {

	/**
	 * 定义在被注解类上的默认缓存名称，用于缓存操作。
	 * <p>
	 * 如果在操作级别未设置任何值，则将使用这些值而不是默认值。
	 * 可用于指定目标缓存，匹配指定值或特定bean定义的bean名称。
	 */
	String[] cacheNames() default {};

	/**
	 * 默认 {@link org.springframework.cache.interceptor.KeyGenerator} 的bean名称。
	 * <p>
	 * 如果在操作级别未设置任何值，则将使用这些值而不是默认值。
	 * key generator和自定义key值互斥。
	 * 如果指定了key值，keyGenerator将会被忽略。
	 */
	String keyGenerator() default "";

	/**
	 * 自定义 {@link org.springframework.cache.CacheManager} 的bean名称，如果未指定将创建默认的
	 * {@link org.springframework.cache.interceptor.CacheResolver}。
	 * <p>
	 * 如果在操作级别没有设置resolver或cache manager，如果没有通过 {@link #cacheResolver} 指定cache resolver，
	 * 将会用这个代替默认。
	 *
	 * @see org.springframework.cache.interceptor.SimpleCacheResolver
	 */
	String cacheManager() default "";

	/**
	 * 自定义 {@link org.springframework.cache.interceptor.CacheResolver} 的名称。
	 * <p>
	 * 如果在操作级别没有设置resolver或cache manager，如果没有通过 {@link #cacheResolver} 指定cache resolver，
	 * 将会用这个代替默认。
	 */
	String cacheResolver() default "";

}
