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

package org.springframework.cache.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.MethodClassKey;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存方法缓存相关属性 {@link CacheOperation} 的抽象实现类，实现了fallback策略：
 * 1. 特定目标方法；
 * 2. 特定类；
 * 3. 声明方法；
 * 4. 声明类/接口；
 * <p>
 * 如果目标方法没有相关的缓存配置，默认使用目标类的缓存配置。目标方法上的缓存属性配置将会
 * 重载目标类上的相同属性。如果在目标类上也没有找到缓存配置，则会去调用该方法的接口上获取
 * （如果是JDK代理实现的话）。
 * <p>
 * 该实现类将在属性第一次被使用之后进行缓存操作。如果想要动态更改缓存属性（不太可能），
 * 可以使缓存可配置。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractFallbackCacheOperationSource implements CacheOperationSource {

	/**
	 * 空缓存
	 * 没有任何缓存配置的方法对应的规范值（空列表），之后无需检查。
	 */
	private static final Collection<CacheOperation> NULL_CACHING_ATTRIBUTE = Collections.emptyList();


	/**
	 * 子类可用的Logger。
	 * 基类没有实现Serializable标记接口，因此具体子类要是Serializable的，在序列化之后会重新生成logger。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * CacheOperation的缓存，由特定目标类的方法指定key。
	 * 基类没有实现Serializable标记接口，因此具体子类要是Serializable的，在序列化之后会重新生成logger。
	 */
	private final Map<Object, Collection<CacheOperation>> attributeCache = new ConcurrentHashMap<>(1024);


	/**
	 * 确定该方法调用的缓存属性。
	 * 方法上未获得属性时，使用所在类上的属性。
	 *
	 * @param method      当前调用的方法 (永不为 {@code null})
	 * @param targetClass 当前调用的目标类 (可能为 {@code null})
	 * @return 当前方法的 {@link CacheOperation} , 如果方法不能缓存则返回 {@code null}
	 */
	@Override
	@Nullable
	public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
		if (method.getDeclaringClass() == Object.class) {
			return null;
		}

		//先通过所给方法获得缓存的key值
		Object cacheKey = getCacheKey(method, targetClass);
		//在已有缓存里看看有没有已经被缓存
		Collection<CacheOperation> cached = this.attributeCache.get(cacheKey);

		if (cached != null) {
			//如果有缓存，则查看是不是空缓存，是的话返回null，不是返回该缓存。
			return (cached != NULL_CACHING_ATTRIBUTE ? cached : null);
		} else {
			//在目标类和方法上寻找缓存操作
			Collection<CacheOperation> cacheOps = computeCacheOperations(method, targetClass);
			if (cacheOps != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Adding cacheable method '" + method.getName() + "' with attribute: " + cacheOps);
				}
				//并做缓存
				this.attributeCache.put(cacheKey, cacheOps);
			} else {
				//没有的话标记空
				this.attributeCache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
			}
			return cacheOps;
		}
	}

	/**
	 * 通过所给方法和目标类确定缓存key。
	 * 重载的方法不能生成相同的key。
	 * 不同实例的相同方法必须生成相同的key。
	 *
	 * @param method      所给方法 (永不为 {@code null})
	 * @param targetClass 目标类 (可能为 {@code null})
	 * @return 缓存key值 (永不为 {@code null})
	 */
	protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
		return new MethodClassKey(method, targetClass);
	}

	@Nullable
	private Collection<CacheOperation> computeCacheOperations(Method method, @Nullable Class<?> targetClass) {
		// 非public方法不能缓存
		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		// 从目标类上获取目标方法
		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
		// 首先尝试在该方法上获取缓存操作
		Collection<CacheOperation> opDef = findCacheOperations(specificMethod);
		if (opDef != null) {
			return opDef;
		}

		// 再fallback到声明该方法的类上获取缓存操作
		opDef = findCacheOperations(specificMethod.getDeclaringClass());
		if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
			return opDef;
		}

		if (specificMethod != method) {
			// 还是没获取到属性，则再fallback，从给定方法上找
			opDef = findCacheOperations(method);
			if (opDef != null) {
				return opDef;
			}
			// 还是没找到，则再fallback到声明给定方法的类上找。
			opDef = findCacheOperations(method.getDeclaringClass());
			if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
				return opDef;
			}
		}
		return null;
	}


	/**
	 * 子类需要实现该方法，返回所给类的缓存属性（如果有的话）
	 *
	 * @param clazz 从该类上获取属性
	 * @return 该类对应的所有缓存操作，如果没有的话返回空 {@code null}
	 */
	@Nullable
	protected abstract Collection<CacheOperation> findCacheOperations(Class<?> clazz);

	/**
	 * 子类需要实现该方法，返回所给方法的缓存属性（如果有的话）
	 *
	 * @param method 从该方法上获取属性
	 * @return 该方法对应的所有缓存操作，如果没有的话返回空 {@code null}
	 */
	@Nullable
	protected abstract Collection<CacheOperation> findCacheOperations(Method method);

	/**
	 * 返回是否只有public方法允许缓存语义。
	 * 默认实现返回{@code false}
	 */
	protected boolean allowPublicMethodsOnly() {
		return false;
	}

}
