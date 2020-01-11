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
	 * <p>
	 * 这相当于：
	 * <pre><code>
	 * ValueWrapper existingValue = cache.get(key);
	 * if (existingValue == null) {
	 *     cache.put(key, value);
	 * }
	 * return existingValue;
	 * </code></pre>
	 * 但是该方法是原子性的。虽然所有开箱即用的{@link CacheManager}实现都可以自动执行缓存添加
	 * 但操作也可能是分非原子性的两步进行的--检查是否存在，再添加缓存。想知道更多细节，请查阅本地
	 * 缓存的文档。默认实现委托给{@link #get(Object)}和{@link #put(Object, Object)}方法。
	 *
	 * @param key   建立联系的key
	 * @param value 建立联系的value
	 * @return 该缓存映射中指定key对应的value（可能value本身为 {@code null} ），也可能在调用
	 * 之前缓存中就没有该key的映射（也会返回{@code null}）。
	 * 因此，返回{@code null}表示给定的{@code value}已与key相关联。
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
	 * 如果缓存中存在的话，移除该key对应的缓存映射。
	 * 实际的缓存移除操作可能是异步或延迟的，所以紧随的查询可能还能查询到该键值对。
	 * 这可以作为缓存事务装饰器的适用情形例子。如果需要保证缓存马上被移除，
	 * 请使用{@link #evictIfPresent}
	 *
	 * @param key 缓存中想要移除的key
	 * @see #evictIfPresent(Object)
	 */
	void evict(Object key);

	/**
	 * 如果缓存中存在的话，移除该key对应的缓存映射，并且该键对紧随的查询操作立马不可见
	 * 默认的实现委托给{@link #evict(Object)}，如果先前存在无法缺点的key，返回
	 * {@code false}.
	 * 鼓励高速缓存提供者，尤其是高速缓存装饰器，如果可能的话
	 * （例如，在事务中通常延迟的高速缓存操作的情况下）执行立即驱逐并可靠地确定给定key的先前存在。
	 *
	 * @param key 缓存中移除的key
	 * @return 如果缓存中之前存在该key的映射返回{@code true}，如果不存在（或为定义）
	 * 则返回{@code false}
	 * @see #evict(Object)
	 * @since 5.2
	 */
	default boolean evictIfPresent(Object key) {
		evict(key);
		return false;
	}

	/**
	 * 通过移除所有映射来清空缓存。
	 * 实际的缓存移除操作可能是异步或延迟的，所以紧随的查询可能还能查询到该键值对。
	 * 这可以作为缓存事务装饰器的适用情形例子。如果需要保证缓存马上被移除，
	 * 如果需要马上移除所有的键值对，请使用{@link #invalidate()}
	 *
	 * @see #invalidate()
	 */
	void clear();

	/**
	 * 通过移除所有映射来使缓存失效，所有的键值对对紧随的查询都立马不可见。
	 *
	 * @return 如果缓存中之前存在映射返回{@code true}，如果不存在（或为定义）
	 * 则返回{@code false}
	 * @see #clear()
	 * @since 5.2
	 */
	default boolean invalidate() {
		clear();
		return false;
	}


	/**
	 * 一个表示缓存值的（包装）对象
	 */
	@FunctionalInterface
	interface ValueWrapper {

		/**
		 * 返回缓存中的实际值
		 */
		@Nullable
		Object get();
	}


	/**
	 * value loader回调失败的情况下，{@link #get(Object, Callable)}抛出的包装异常
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
