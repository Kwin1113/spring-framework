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
import java.util.concurrent.Callable;

/**
 * 该注解表示调用方法的返回结果（或该类的所有方法）可以被缓存
 * <p>
 * 每次调用被该注解增强的方法，将会应用缓存操作，检查该方法是否已以所给参数调用，默认情况下
 * 用所给参数简单地生成缓存的key，或者通过{@link #key}提供一个SpEL表达式，再或者通过自定义
 * 的{@link org.springframework.cache.interceptor.KeyGenerator}实现类来替换默认的实现类
 * (参照 {@link #keyGenerator})。
 * <p>
 * 如果该key没有对应的value，目标方法将会被调用，并且把返回值存储在对应的缓存中。注意Java8中的
 * {@code Optional}返回类型将会被自动处理，并且内容存储在缓存中（如果存在）。
 * <p>
 * 该注解可以被用作自定义组合注解的源注解。
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @see CacheConfig
 * @since 3.1
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Cacheable {

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
	 * 通过SpEL表达式指定方法缓存的条件（满足unless条件就不缓存）。
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
	 *
	 * @since 3.2
	 */
	String unless() default "";

	/**
	 * 在多个线程同时尝试加载同一个key的缓存是，同步方法的调用（即只有一个线程能够做缓存，其他需要等待）。
	 * 同步有以下限制条件：
	 * <p>
	 * 不支持 {@link #unless()}
	 * 只能指定一个缓存
	 * 不能和其他缓存相关的操作组合
	 * <p>
	 * 这只是一个提示，使用的缓存可能并不支持同步。有关的详细信息，请查看缓存提供商的文档。
	 *
	 * @see org.springframework.cache.Cache#get(Object, Callable)
	 * @since 4.3
	 */
	boolean sync() default false;

}
