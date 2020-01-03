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

package org.springframework.cache.annotation;

import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * 解析已有缓存注解类型的策略接口。
 * {@link AnnotationCacheOperationSource} 通过此类解析器以支持特定的注解类型，
 * 例如Spring自己的注解类型：
 * {@link Cacheable}, {@link CachePut} and{@link CacheEvict}.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @see AnnotationCacheOperationSource
 * @see SpringCacheAnnotationParser
 * @since 3.1
 */
public interface CacheAnnotationParser {

	/**
	 * 判断所给类是否满足 {@code CacheAnnotationParser} 注解格式的缓存操作类。
	 * <p>
	 * 如果方法返回 {@code false}, 所给类的方法不会被 {@code #parseCacheAnnotations}
	 * 自检遍历。
	 * 因此返回 {@code false} 是对一些不受影响的类的优化，而 {@code true} 仅表示该类
	 * 需要分别对给定类上的每个方法进行检查。
	 *
	 * @param targetClass 检查的类
	 * @return 如果该类在类或方法级别上都没有缓存操作的注解，返回 {@code false}；
	 * 否则返回 {@code true} 。
	 * 默认实现类返回 {@code true}， 全部进行检查。
	 * @since 5.2
	 */
	default boolean isCandidateClass(Class<?> targetClass) {
		return true;
	}

	/**
	 * 解析基于所给类并能被该解析器解析的缓存定义。
	 * 本质上把一个已知的缓存注解解析成Spring的元数据属性类 metadata attribute class。
	 * 如果该类无法缓存，返回 {@code null}。
	 *
	 * @param type 被注解的类
	 * @return 返回配置类, 如果找不到配置返回 {@code null} 。
	 * @see AnnotationCacheOperationSource#findCacheOperations(Class)
	 */
	@Nullable
	Collection<CacheOperation> parseCacheAnnotations(Class<?> type);

	/**
	 * 解析基于所给方法并能被该解析器解析的缓存定义。
	 * 本质上把一个已知的缓存注解解析成Spring的元数据属性类 metadata attribute class。
	 * 如果该方法无法缓存，返回 {@code null}。
	 *
	 * @param method 被注解的方法
	 * @return 返回配置类, 如果找不到配置返回 {@code null} 。
	 * @see AnnotationCacheOperationSource#findCacheOperations(Method)
	 */
	@Nullable
	Collection<CacheOperation> parseCacheAnnotations(Method method);

}
