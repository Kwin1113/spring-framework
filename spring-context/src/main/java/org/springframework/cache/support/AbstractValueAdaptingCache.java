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

package org.springframework.cache.support;

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

/**
 * {@link Cache}的实现类，在存储到底层存储之前需要兼容调整{@code null}值
 * （和其他潜在的特殊值）
 * <p>
 * 如果支持{@code null}值（如{@link #isAllowNullValues()}所示），则用内部
 * 的{@link NullValue#INSTANCE}替换。
 *
 * @author Juergen Hoeller
 * @since 4.2.2
 */
public abstract class AbstractValueAdaptingCache implements Cache {

	//是否允许保存null
	private final boolean allowNullValues;


	/**
	 * 通过给定设置创建一个{@code AbstractValueAdaptingCache}
	 *
	 * @param allowNullValues 是否允许{@code null}
	 */
	protected AbstractValueAdaptingCache(boolean allowNullValues) {
		this.allowNullValues = allowNullValues;
	}


	/**
	 * 返回是否允许缓存{@code null}
	 */
	public final boolean isAllowNullValues() {
		return this.allowNullValues;
	}

	@Override
	@Nullable
	public ValueWrapper get(Object key) {
		Object value = lookup(key);
		return toValueWrapper(value);
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T get(Object key, @Nullable Class<T> type) {
		//处理一下查询出来的包装类
		Object value = fromStoreValue(lookup(key));
		if (value != null && type != null && !type.isInstance(value)) {
			throw new IllegalStateException(
					"Cached value is not of required type [" + type.getName() + "]: " + value);
		}
		return (T) value;
	}

	/**
	 * 在底层存储中执行实际的查找
	 *
	 * @param key 返回值对应的key
	 * @return 该key对应的未经处理的存储值，不存在则为{@code null}
	 */
	@Nullable
	protected abstract Object lookup(Object key);


	/**
	 * 将内部存储中的给定值转换为get方法返回的用户值（适应{@code null}）。
	 *
	 * @param storeValue 内部存储值
	 * @return 返回给用户的值
	 */
	@Nullable
	protected Object fromStoreValue(@Nullable Object storeValue) {
		if (this.allowNullValues && storeValue == NullValue.INSTANCE) {
			return null;
		}
		return storeValue;
	}

	/**
	 * 将传递给put方法的给定用户值转换为内部存储区中的值（适应{@code null}）。
	 * @param userValue 用户给定的值
	 * @return 存储的值
	 */
	protected Object toStoreValue(@Nullable Object userValue) {
		if (userValue == null) {
			if (this.allowNullValues) {
				return NullValue.INSTANCE;
			}
			throw new IllegalArgumentException(
					"Cache '" + getName() + "' is configured to not allow null values but null was provided");
		}
		return userValue;
	}

	/**
	 * 使用{@link SimpleValueWrapper}包装给定的存储值，同时经过{@link #fromStoreValue}
	 * 转换。对{@link #get(Object)}和{@link #putIfAbsent(Object, Object)}实现很有用
	 *
	 * @param storeValue 原始值
	 * @return 包装后的值
	 */
	@Nullable
	protected Cache.ValueWrapper toValueWrapper(@Nullable Object storeValue) {
		return (storeValue != null ? new SimpleValueWrapper(fromStoreValue(storeValue)) : null);
	}


}
