/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.cache.concurrent;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.serializer.support.SerializationDelegate;
import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link CacheManager}的实现类，为每个{@link #getCache}请求以懒汉方式创建一个
 * {@link ConcurrentMapCache}实例。也支持通过{@link #setCacheNames}提前定义
 * 的'静态'模式，在运行时不会再动态创建缓存。
 * <p>
 * 注意：但这不是很复杂的CacheManager；它没有缓存配置选项。但是，它可能再测试或简单的
 * 缓存场景中很有用。如果想要更高级的需求，考虑使用
 * {@link org.springframework.cache.jcache.JCacheCacheManager},
 * {@link org.springframework.cache.ehcache.EhCacheCacheManager},
 * {@link org.springframework.cache.caffeine.CaffeineCacheManager}.
 *
 * @author Juergen Hoeller
 * @see ConcurrentMapCache
 * @since 3.1
 */
public class ConcurrentMapCacheManager implements CacheManager, BeanClassLoaderAware {

	//也是一个String->Cache的线程安全Map集合
	private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

	private boolean dynamic = true;

	private boolean allowNullValues = true;

	private boolean storeByValue = false;

	@Nullable
	private SerializationDelegate serialization;


	/**
	 * 构建一个动态的ConcurrentMapCacheManager，
	 * 请求时以懒汉模式创建缓存实例。
	 */
	public ConcurrentMapCacheManager() {
	}

	/**
	 * 构建一个静态的ConcurrentMapCacheManager，
	 * 只管理指定名称的缓存。
	 */
	public ConcurrentMapCacheManager(String... cacheNames) {
		setCacheNames(Arrays.asList(cacheNames));
	}


	/**
	 * 指定该CacheManager'静态'模式的缓存名称集合。
	 * 缓存的名称和数量将会在调用该方法之后固定不变，在之后的运行过程中不在添加缓存。
	 * 传参{@code null}来重置回'动态'模式，允许在运行过程中动态添加缓存。
	 */
	public void setCacheNames(@Nullable Collection<String> cacheNames) {
		if (cacheNames != null) {
			//添加缓存，并设置dynamic为false
			for (String name : cacheNames) {
				this.cacheMap.put(name, createConcurrentMapCache(name));
			}
			this.dynamic = false;
		} else {
			//如果参数为null，则把dynamic改为true
			this.dynamic = true;
		}
	}

	/**
	 * 指定该缓存管理器下的所有缓存是否接受或转换{@code null}值。
	 * 默认为"true"，不管ConcurrentHashMap本身是否支持 {@code null}。
	 * 内部持有对象将用于存储用户级别的{@code null}。
	 *
	 * <p>Note: A change of the null-value setting will reset all existing caches,
	 * if any, to reconfigure them with the new null-value requirement.
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		if (allowNullValues != this.allowNullValues) {
			this.allowNullValues = allowNullValues;
			// Need to recreate all Cache instances with the new null-value configuration...
			recreateCaches();
		}
	}

	/**
	 * 返回该缓存管理器是否接受或转换{@code null}值。
	 */
	public boolean isAllowNullValues() {
		return this.allowNullValues;
	}

	/**
	 * 指定该缓存管理器是存储每个键值对的副本{@code true}还是存储缓存的应用{@code false}。
	 * 默认值为“ false”，这样就可以存储值本身，并且不需要对缓存的值进行可序列化的约定。
	 *
	 * <p>Note: A change of the store-by-value setting will reset all existing caches,
	 * if any, to reconfigure them with the new store-by-value requirement.
	 *
	 * @since 4.3
	 */
	public void setStoreByValue(boolean storeByValue) {
		if (storeByValue != this.storeByValue) {
			this.storeByValue = storeByValue;
			// Need to recreate all Cache instances with the new store-by-value configuration...
			recreateCaches();
		}
	}

	/**
	 * 该缓存管理器是存储每个键值对的副本{@code true}还是存储缓存的应用{@code false}。
	 * 如果是{@code true}，则需要序列化所有键值对。
	 *
	 * @since 4.3
	 */
	public boolean isStoreByValue() {
		return this.storeByValue;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.serialization = new SerializationDelegate(classLoader);
		// Need to recreate all Cache instances with new ClassLoader in store-by-value mode...
		if (isStoreByValue()) {
			recreateCaches();
		}
	}


	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(this.cacheMap.keySet());
	}

	@Override
	@Nullable
	public Cache getCache(String name) {
		//从已有缓存中取
		Cache cache = this.cacheMap.get(name);
		if (cache == null && this.dynamic) {
			//如果缓存不存在并且支持动态则双重校验创建并增加缓存
			synchronized (this.cacheMap) {
				cache = this.cacheMap.get(name);
				if (cache == null) {
					cache = createConcurrentMapCache(name);
					this.cacheMap.put(name, cache);
				}
			}
		}
		//不支持动态直接返回null
		return cache;
	}

	private void recreateCaches() {
		for (Map.Entry<String, Cache> entry : this.cacheMap.entrySet()) {
			entry.setValue(createConcurrentMapCache(entry.getKey()));
		}
	}

	/**
	 * 创建一个指定缓存名称的ConcurrentMapCache
	 *
	 * @param name 缓存名称
	 * @return ConcurrentMapCache（或其装饰器）
	 */
	protected Cache createConcurrentMapCache(String name) {
		SerializationDelegate actualSerialization = (isStoreByValue() ? this.serialization : null);
		return new ConcurrentMapCache(name, new ConcurrentHashMap<>(256),
				isAllowNullValues(), actualSerialization);

	}

}
