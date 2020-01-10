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

package org.springframework.cache;

import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

/**
 * 定义了通用缓存操作的接口
 * <p>
 * 注意：由于缓存的一般用法，建议实现允许存储空值（例如，缓存返回{@code null}的方法）。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public interface Cache {

	/**
	 * 返回缓存名称
	 */
	String getName();

	/**
	 * 返回底层的本地缓存提供者
	 */
	Object getNativeCache();

	/**
	 * 返回缓存中该特定key对应的值。
	 * 如果缓存中该key没有映射，则返回{@code null}；否则，缓存的值
	 * （本身可能为{@code null}）将在{@link ValueWrapper}中返回。
	 *
	 * @param key 返回值对应的key
	 * @return 返回缓存中该特定key对应的值，缓存的值本身可能为{@code null}）
	 * 将在{@link ValueWrapper}中返回。直接返回{@code null}表示缓存中
	 * 没有该key的映射
	 * @see #get(Object, Class)
	 * @see #get(Object, Callable)
	 */
	@Nullable
	ValueWrapper get(Object key);

	/**
	 * 返回缓存中该特定key对应的值。
	 * 一般指定一个返回值的类型来将其转型。
	 * 注意：该{@code get}方法的变体无法区分缓存的{@code null}和无法找到缓存的情况
	 * 如果想要区分，使用标准的{@link #get(Object)}方法
	 *
	 * @param key  返回值对应的key
	 * @param type 返回值的类型（可以是{@code null}来绕过类型检查）
	 *             如果在缓存中找到{@code null}值，则指定的类型无关紧要
	 * @return 返回缓存中该特定key对应的值，缓存的值本身可能为{@code null}）
	 * 将在{@link ValueWrapper}中返回。直接返回{@code null}表示缓存中
	 * 没有该key的映射
	 * @throws IllegalStateException 如果找到了一个缓存键值对但是无法匹配指定的类型
	 * @see #get(Object)
	 * @since 4.0
	 */
	@Nullable
	<T> T get(Object key, @Nullable Class<T> type);

	/**
	 * 返回缓存中该特定key对应的值，有必要的话从{@code valueLoader}获取值。
	 * 该方法提供了一个常规模式（if cached, return; otherwise create, cache and return）
	 * 的简单替代。
	 * 如果可能的话，实现类应该在加载操作时是同步的，保证多个线程在同一个key的情况下特定的
	 * {@code valueLoader}只被调用一次。
	 * 如果{@code valueLoader}抛出异常，将会被{@link ValueRetrievalException}包装处理。
	 *
	 * @param key 返回值对应的key
	 * @return 缓存中匹配指定key的值
	 * @throws ValueRetrievalException 如果{@code valueLoader}抛出异常
	 * @see #get(Object)
	 * @since 4.3
	 */
	@Nullable
	<T> T get(Object key, Callable<T> valueLoader);

	/**
	 * 在该缓存中，将给定key和给定value联系起来。
	 * 如果缓存中之前存在该key的映射，旧的value将会被给定的value替换。
	 * 实际的注册可以通过异步或延迟的方式执行，金穗的查找可能暂时无法找到
	 * 该缓存键值对。这是事务缓存装饰器transactional cache decorators的一种
	 * 示例情况。可以使用{@link #putIfAbsent}保证立即注册。
	 *
	 * @param key   映射关系中的key
	 * @param value 映射关系的value
	 * @see #putIfAbsent(Object, Object)
	 */
	void put(Object key, @Nullable Object value);

	/**
	 * 自动建立缓存中给定key和给定value的联系（如果联系未建立）
	 * Atomically associate the specified value with the specified key in this cache
	 * if it is not set already.
	 * <p>This is equivalent to:
	 * <pre><code>
	 * ValueWrapper existingValue = cache.get(key);
	 * if (existingValue == null) {
	 *     cache.put(key, value);
	 * }
	 * return existingValue;
	 * </code></pre>
	 * except that the action is performed atomically. While all out-of-the-box
	 * {@link CacheManager} implementations are able to perform the put atomically,
	 * the operation may also be implemented in two steps, e.g. with a check for
	 * presence and a subsequent put, in a non-atomic way. Check the documentation
	 * of the native cache implementation that you are using for more details.
	 * <p>The default implementation delegates to {@link #get(Object)} and
	 * {@link #put(Object, Object)} along the lines of the code snippet above.
	 *
	 * @param key   the key with which the specified value is to be associated
	 * @param value the value to be associated with the specified key
	 * @return the value to which this cache maps the specified key (which may be
	 * {@code null} itself), or also {@code null} if the cache did not contain any
	 * mapping for that key prior to this call. Returning {@code null} is therefore
	 * an indicator that the given {@code value} has been associated with the key.
	 * @see #put(Object, Object)
	 * @since 4.1
	 */
	@Nullable
	default ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		ValueWrapper existingValue = get(key);
		if (existingValue == null) {
			put(key, value);
		}
		return existingValue;
	}

	/**
	 * Evict the mapping for this key from this cache if it is present.
	 * <p>Actual eviction may be performed in an asynchronous or deferred
	 * fashion, with subsequent lookups possibly still seeing the entry.
	 * This may for example be the case with transactional cache decorators.
	 * Use {@link #evictIfPresent} for guaranteed immediate removal.
	 *
	 * @param key the key whose mapping is to be removed from the cache
	 * @see #evictIfPresent(Object)
	 */
	void evict(Object key);

	/**
	 * Evict the mapping for this key from this cache if it is present,
	 * expecting the key to be immediately invisible for subsequent lookups.
	 * <p>The default implementation delegates to {@link #evict(Object)},
	 * returning {@code false} for not-determined prior presence of the key.
	 * Cache providers and in particular cache decorators are encouraged
	 * to perform immediate eviction if possible (e.g. in case of generally
	 * deferred cache operations within a transaction) and to reliably
	 * determine prior presence of the given key.
	 *
	 * @param key the key whose mapping is to be removed from the cache
	 * @return {@code true} if the cache was known to have a mapping for
	 * this key before, {@code false} if it did not (or if prior presence
	 * could not be determined)
	 * @see #evict(Object)
	 * @since 5.2
	 */
	default boolean evictIfPresent(Object key) {
		evict(key);
		return false;
	}

	/**
	 * Clear the cache through removing all mappings.
	 * <p>Actual clearing may be performed in an asynchronous or deferred
	 * fashion, with subsequent lookups possibly still seeing the entries.
	 * This may for example be the case with transactional cache decorators.
	 * Use {@link #invalidate()} for guaranteed immediate removal of entries.
	 *
	 * @see #invalidate()
	 */
	void clear();

	/**
	 * Invalidate the cache through removing all mappings, expecting all
	 * entries to be immediately invisible for subsequent lookups.
	 *
	 * @return {@code true} if the cache was known to have mappings before,
	 * {@code false} if it did not (or if prior presence of entries could
	 * not be determined)
	 * @see #clear()
	 * @since 5.2
	 */
	default boolean invalidate() {
		clear();
		return false;
	}


	/**
	 * A (wrapper) object representing a cache value.
	 */
	@FunctionalInterface
	interface ValueWrapper {

		/**
		 * Return the actual value in the cache.
		 */
		@Nullable
		Object get();
	}


	/**
	 * Wrapper exception to be thrown from {@link #get(Object, Callable)}
	 * in case of the value loader callback failing with an exception.
	 *
	 * @since 4.3
	 */
	@SuppressWarnings("serial")
	class ValueRetrievalException extends RuntimeException {

		@Nullable
		private final Object key;

		public ValueRetrievalException(@Nullable Object key, Callable<?> loader, Throwable ex) {
			super(String.format("Value for key '%s' could not be loaded using '%s'", key, loader), ex);
			this.key = key;
		}

		@Nullable
		public Object getKey() {
			return this.key;
		}
	}

}
