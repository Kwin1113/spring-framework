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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;

import org.springframework.core.annotation.AliasFor;

/**
 * Annotation indicating that the result of invoking a method (or all methods
 * in a class) can be cached.
 * 该注解表示调用方法的返回结果（或该类的所有方法）可以被缓存
 *
 * <p>Each time an advised method is invoked, caching behavior will be applied,
 * checking whether the method has been already invoked for the given arguments.
 * A sensible default simply uses the method parameters to compute the key, but
 * a SpEL expression can be provided via the {@link #key} attribute, or a custom
 * {@link org.springframework.cache.interceptor.KeyGenerator} implementation can
 * replace the default one (see {@link #keyGenerator}).
 * 每次调用被该注解增强的方法，将会应用缓存操作，检查该方法是否已以所给参数调用，默认情况下
 * 用所给参数简单地生成缓存的key，或者通过{@link #key}提供一个SpEL表达式，再或者通过自定义
 * 的{@link org.springframework.cache.interceptor.KeyGenerator}实现类来替换默认的实现类
 * (参照 {@link #keyGenerator})。
 *
 * <p>If no value is found in the cache for the computed key, the target method
 * will be invoked and the returned value stored in the associated cache. Note
 * that Java8's {@code Optional} return types are automatically handled and its
 * content is stored in the cache if present.
 * 如果该key没有对应的value，目标方法将会被调用，并且把返回值存储在对应的缓存中。注意Java8中的
 * {@code Optional}返回类型将会被自动处理，并且内容存储在缓存中（如果存在）。
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em> with attribute overrides.
 * 该注释可以被用作自定义组合注释的源注释。
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.1
 * @see CacheConfig
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Cacheable {

	/**
	 * Alias for {@link #cacheNames}.
	 * {@link #cacheNames}的别名。
	 */
	@AliasFor("cacheNames")
	String[] value() default {};

	/**
	 * Names of the caches in which method invocation results are stored.
	 * <p>Names may be used to determine the target cache (or caches), matching
	 * the qualifier value or bean name of a specific bean definition.
	 * 方法返回值所存储的缓存名。缓存名将用于决定目标缓存，匹配特定的bean定义或者bean名称。
	 * @since 4.2
	 * @see #value
	 * @see CacheConfig#cacheNames
	 */
	@AliasFor("value")
	String[] cacheNames() default {};

	/**
	 * Spring Expression Language (SpEL) expression for computing the key dynamically.
	 * <p>Default is {@code ""}, meaning all method parameters are considered as a key,
	 * unless a custom {@link #keyGenerator} has been configured.
	 * SqEL表达式用于动态计算缓存key值。
	 * 默认为""，在没有配置自定义{@link #keyGenerator}得情况下表示所有方法参数都会被用来做key计算。
	 * <p>The SpEL expression evaluates against a dedicated context that provides the
	 * following meta-data:
	 * <ul>
	 * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
	 * references to the {@link java.lang.reflect.Method method}, target object, and
	 * affected cache(s) respectively.</li>
	 * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
	 * ({@code #root.targetClass}) are also available.
	 * <li>Method arguments can be accessed by index. For instance the second argument
	 * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
	 * can also be accessed by name if that information is available.</li>
	 * </ul>
	 */
	String key() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.interceptor.KeyGenerator}
	 * to use.
	 * <p>Mutually exclusive with the {@link #key} attribute.
	 * @see CacheConfig#keyGenerator
	 */
	String keyGenerator() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.CacheManager} to use to
	 * create a default {@link org.springframework.cache.interceptor.CacheResolver} if none
	 * is set already.
	 * <p>Mutually exclusive with the {@link #cacheResolver}  attribute.
	 * @see org.springframework.cache.interceptor.SimpleCacheResolver
	 * @see CacheConfig#cacheManager
	 */
	String cacheManager() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.interceptor.CacheResolver}
	 * to use.
	 * @see CacheConfig#cacheResolver
	 */
	String cacheResolver() default "";

	/**
	 * Spring Expression Language (SpEL) expression used for making the method
	 * caching conditional.
	 * <p>Default is {@code ""}, meaning the method result is always cached.
	 * <p>The SpEL expression evaluates against a dedicated context that provides the
	 * following meta-data:
	 * <ul>
	 * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
	 * references to the {@link java.lang.reflect.Method method}, target object, and
	 * affected cache(s) respectively.</li>
	 * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
	 * ({@code #root.targetClass}) are also available.
	 * <li>Method arguments can be accessed by index. For instance the second argument
	 * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
	 * can also be accessed by name if that information is available.</li>
	 * </ul>
	 */
	String condition() default "";

	/**
	 * Spring Expression Language (SpEL) expression used to veto method caching.
	 * <p>Unlike {@link #condition}, this expression is evaluated after the method
	 * has been called and can therefore refer to the {@code result}.
	 * <p>Default is {@code ""}, meaning that caching is never vetoed.
	 * <p>The SpEL expression evaluates against a dedicated context that provides the
	 * following meta-data:
	 * <ul>
	 * <li>{@code #result} for a reference to the result of the method invocation. For
	 * supported wrappers such as {@code Optional}, {@code #result} refers to the actual
	 * object, not the wrapper</li>
	 * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
	 * references to the {@link java.lang.reflect.Method method}, target object, and
	 * affected cache(s) respectively.</li>
	 * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
	 * ({@code #root.targetClass}) are also available.
	 * <li>Method arguments can be accessed by index. For instance the second argument
	 * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
	 * can also be accessed by name if that information is available.</li>
	 * </ul>
	 * @since 3.2
	 */
	String unless() default "";

	/**
	 * Synchronize the invocation of the underlying method if several threads are
	 * attempting to load a value for the same key. The synchronization leads to
	 * a couple of limitations:
	 * <ol>
	 * <li>{@link #unless()} is not supported</li>
	 * <li>Only one cache may be specified</li>
	 * <li>No other cache-related operation can be combined</li>
	 * </ol>
	 * This is effectively a hint and the actual cache provider that you are
	 * using may not support it in a synchronized fashion. Check your provider
	 * documentation for more details on the actual semantics.
	 * @since 4.3
	 * @see org.springframework.cache.Cache#get(Object, Callable)
	 */
	boolean sync() default false;

}
