/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 指示方法（或类中的所有方法）触发缓存删除操作的注解。
 * <p>
 * 该注解可以被用作自定义组合注解的源注解。
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @see CacheConfig
 * @since 3.1
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheEvict {

	/**
	 * {@link #cacheNames}的别名。
	 */
	@AliasFor("cacheNames")
	String[] value() default {};

	/**
	 * 方法返回值所存储的缓存名。缓存名将用于决定目标缓存，匹配特定的bean定义或者bean名称。
	 *
	 * @see #value
	 * @see CacheConfig#cacheNames
	 * @since 4.2
	 */
	@AliasFor("value")
	String[] cacheNames() default {};

	/**
	 * SqEL表达式用于动态计算缓存key值。
	 * 默认为""，在没有配置自定义{@link #keyGenerator}得情况下表示所有方法参数都会被用来做key计算。
	 * <p>
	 * SpEl表达式通过以下提供的元数据来计算特有的上下文。
	 * <p>
	 * {@code #root.method}, {@code #root.target}, 和 {@code #root.caches}分别用于引用方法、
	 * 目标对象和受影响的缓存。
	 * 方法名({@code #root.methodName}) 和目标类 ({@code #root.targetClass}) 也可以用作key。
	 * <p>
	 * 方法参数也可以用索引来引用。比如可以通过{@code #root.args[1]}, {@code #p1} 或 {@code #a1}
	 * 的方式来获取第二个参数。当然也可以通过参数名称来获取。
	 */
	String key() default "";

	/**
	 * 自定义 {@link org.springframework.cache.interceptor.KeyGenerator} 的bean名称。
	 * 和 {@link #key} 属性互斥。
	 *
	 * @see CacheConfig#keyGenerator
	 */
	String keyGenerator() default "";

	/**
	 * 自定义 {@link org.springframework.cache.CacheManager} 的bean名称，如果未指定将创建默认的
	 * {@link org.springframework.cache.interceptor.CacheResolver}。
	 * 和 {@link #cacheResolver} 属性互斥。
	 *
	 * @see org.springframework.cache.interceptor.SimpleCacheResolver
	 * @see CacheConfig#cacheManager
	 */
	String cacheManager() default "";

	/**
	 * 自定义 {@link org.springframework.cache.interceptor.CacheResolver} 的bean名称。
	 *
	 * @see CacheConfig#cacheResolver
	 */
	String cacheResolver() default "";

	/**
	 * 通过SpEL表达式指定方法缓存的条件（满足condition条件就缓存）。
	 * 默认为空{@code ""}, 表示无条件缓存。
	 * <p>
	 * SpEl表达式通过以下提供的元数据来计算特有的上下文。
	 * <p>
	 * {@code #root.method}, {@code #root.target}, 和 {@code #root.caches}分别用于引用方法、
	 * 目标对象和受影响的缓存。
	 * 方法名({@code #root.methodName}) 和目标类 ({@code #root.targetClass}) 也可以用作key。
	 * <p>
	 * 方法参数也可以用索引来引用。比如可以通过{@code #root.args[1]}, {@code #p1} 或 {@code #a1}
	 * 的方式来获取第二个参数。当然也可以通过参数名称来获取。
	 */
	String condition() default "";

	/**
	 * 是否清空缓存中的所有键值对。
	 * 默认情况下，只有关联key对应的value会被移除。
	 * 提示：不能把该属性设置成 {@code true} ，并同时指定一个 {@link #key}。
	 */
	boolean allEntries() default false;

	/**
	 * 缓存移除操作是否在方法被调用之前进行。
	 * 如果把该属性设置为 {@code true}， 不管方法返回什么，都会进行缓存移除操作（比如，
	 * 不管是否抛出异常）。
	 * 默认是 {@code false}，表示缓存移除操作会在被增强的方法成功调用之后进行。（比如，
	 * 只有在方法调用不抛出异常的情况下）。
	 */
	boolean beforeInvocation() default false;

}
