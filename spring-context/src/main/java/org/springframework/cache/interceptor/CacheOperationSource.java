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

import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * {@link CacheInterceptor} 的接口。实现类需要实现缓存操作属性的配置，可能来自配置、资源级别的
 * 元数据属性或其他地方。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
public interface CacheOperationSource {

	/**
	 * 确定在 {@code CacheOperationSource} 的元数据格式下，所给类是否为缓存操作类的潜在类。
	 * 如果该方法返回{@code false}，所给类的方法不会被 {@link #getCacheOperations} 检查
	 * 时遍历到。因此返回{@code false}是对无影响类的优化，然后返回{@code true}仅仅代表该类
	 * 上的每个方法都需要被充分的检查。
	 *
	 * @param targetClass 自检目标类
	 * @return 如果所给类在类级别或方法级别没有缓存操作元数据 cache operation metadata
	 * 则返回 {@code false};
	 * 否则返回 {@code true}。
	 * 默认实现类返回{@code true}，检查所有类和方法。
	 * @since 5.2
	 */
	default boolean isCandidateClass(Class<?> targetClass) {
		return true;
	}

	/**
	 * 返回该方法的缓存操作集合，如果方法上没有cacheable注解（并不只是{@link org.springframework.cache.annotation.Cacheable}),
	 * 返回{@code null}。
	 *
	 * @param method      检查的目标方法
	 * @param targetClass 目标类（可以为空{@code null}，此时目标类为方法所在类）
	 * @return 该方法的所有缓存操作集合，不存在时返回{@code null}
	 */
	@Nullable
	Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass);

}
