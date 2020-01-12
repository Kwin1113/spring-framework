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

package org.springframework.cache.concurrent;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.core.serializer.support.SerializationDelegate;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link org.springframework.cache.Cache}的简单实现类，
 * 基于JDK核心包{@code java.util.concurrent}实现。
 * <p>
 * 在测试或简单的场景下很有用，通常与{@link org.springframework.cache.support.SimpleCacheManager}
 * 结合使用，或通过{@link ConcurrentMapCacheManager}动态使用。
 * <p>
 * 注意：因为{@link ConcurrentHashMap}）（默认实现使用的存储方式）不支持存储{@code null}值，
 * 该类将用预先定义的内部类替换。但可以通过{@link #ConcurrentMapCache(String, ConcurrentMap, boolean)}
 * 构造器选择不这么做。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public class ConcurrentMapCache extends AbstractValueAdaptingCache {

	private final String name;

	private final ConcurrentMap<Object, Object> store;

	@Nullable
	private final SerializationDelegate serialization;


	/**
	 * 通过给定的名称创建一个ConcurrentMapCache
	 *
	 * @param name 缓存的名称
	 */
	public ConcurrentMapCache(String name) {
		this(name, new ConcurrentHashMap<>(256), true);
	}

	/**
	 * 通过给定的名称创建要给新的ConcurrentMapCache
	 *
	 * @param name            缓存的名称
	 * @param allowNullValues 是否接受和转换{@code null}值
	 */
	public ConcurrentMapCache(String name, boolean allowNullValues) {
		this(name, new ConcurrentHashMap<>(256), allowNullValues);
	}

	/**
	 * 通过给定的名称和内部{@link ConcurrentMap}创建一个新的ConcurrentMapCache
	 *
	 * @param name            缓存名称
	 * @param store           用于内部存储的ConcurrentMap
	 * @param allowNullValues 是否接受和转换{@code null}值
	 */
	public ConcurrentMapCache(String name, ConcurrentMap<Object, Object> store, boolean allowNullValues) {
		this(name, store, allowNullValues, null);
	}

	/**
	 * 通过给定的名称和内部{@link ConcurrentMap}创建一个新的ConcurrentMapCache
	 * 如果指定了{@link SerializationDelegate},则开启
	 * {@link #isStoreByValue() store-by-value}
	 *
	 * @param name            缓存名称
	 * @param store           用于内部存储的ConcurrentMap
	 * @param allowNullValues 是否接受和转换{@code null}值
	 *                        (将他们兼容成内部持有null的值)
	 * @param serialization   使用的{@link SerializationDelegate}，用于序列化缓存键值对
	 *                        或{@code null}存储引用
	 * @since 4.3
	 */
	protected ConcurrentMapCache(String name, ConcurrentMap<Object, Object> store,
								 boolean allowNullValues, @Nullable SerializationDelegate serialization) {

		super(allowNullValues);
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(store, "Store must not be null");
		this.name = name;
		this.store = store;
		this.serialization = serialization;
	}


	/**
	 * 返回缓存存储是存储每个键值对的副本（{@code true}）还是存储应用（{@code false}，默认）。
	 * 如果存储副本则需要序列化每个键值对。
	 *
	 * @since 4.3
	 */
	public final boolean isStoreByValue() {
		return (this.serialization != null);
	}

	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public final ConcurrentMap<Object, Object> getNativeCache() {
		return this.store;
	}

	@Override
	@Nullable
	protected Object lookup(Object key) {
		return this.store.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T get(Object key, Callable<T> valueLoader) {
		return (T) fromStoreValue(this.store.computeIfAbsent(key, k -> {
			try {
				return toStoreValue(valueLoader.call());
			} catch (Throwable ex) {
				throw new ValueRetrievalException(key, valueLoader, ex);
			}
		}));
	}

	@Override
	public void put(Object key, @Nullable Object value) {
		this.store.put(key, toStoreValue(value));
	}

	@Override
	@Nullable
	public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		Object existing = this.store.putIfAbsent(key, toStoreValue(value));
		return toValueWrapper(existing);
	}

	@Override
	public void evict(Object key) {
		this.store.remove(key);
	}

	@Override
	public boolean evictIfPresent(Object key) {
		return (this.store.remove(key) != null);
	}

	@Override
	public void clear() {
		this.store.clear();
	}

	@Override
	public boolean invalidate() {
		boolean notEmpty = !this.store.isEmpty();
		this.store.clear();
		return notEmpty;
	}

	@Override
	protected Object toStoreValue(@Nullable Object userValue) {
		Object storeValue = super.toStoreValue(userValue);
		if (this.serialization != null) {
			try {
				return serializeValue(this.serialization, storeValue);
			} catch (Throwable ex) {
				throw new IllegalArgumentException("Failed to serialize cache value '" + userValue +
						"'. Does it implement Serializable?", ex);
			}
		} else {
			return storeValue;
		}
	}

	private Object serializeValue(SerializationDelegate serialization, Object storeValue) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			serialization.serialize(storeValue, out);
			return out.toByteArray();
		} finally {
			out.close();
		}
	}

	@Override
	protected Object fromStoreValue(@Nullable Object storeValue) {
		if (storeValue != null && this.serialization != null) {
			try {
				return super.fromStoreValue(deserializeValue(this.serialization, storeValue));
			} catch (Throwable ex) {
				throw new IllegalArgumentException("Failed to deserialize cache value '" + storeValue + "'", ex);
			}
		} else {
			return super.fromStoreValue(storeValue);
		}

	}

	private Object deserializeValue(SerializationDelegate serialization, Object storeValue) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream((byte[]) storeValue);
		try {
			return serialization.deserialize(in);
		} finally {
			in.close();
		}
	}

}
