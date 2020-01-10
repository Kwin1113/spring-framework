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

import java.io.Serializable;
import java.util.Arrays;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link SimpleKeyGenerator}返回的简单的key
 *
 * @author Phillip Webb
 * @since 4.0
 * @see SimpleKeyGenerator
 */
@SuppressWarnings("serial")
public class SimpleKey implements Serializable {

	/** 空key. */
	public static final SimpleKey EMPTY = new SimpleKey();


	private final Object[] params;

	private final int hashCode;


	/**
	 * 创建一个新{@link SimpleKey}实例。
	 * @param elements 构造key的元素
	 */
	public SimpleKey(Object... elements) {
		Assert.notNull(elements, "Elements must not be null");
		this.params = elements.clone();
		this.hashCode = Arrays.deepHashCode(this.params);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other ||
				(other instanceof SimpleKey && Arrays.deepEquals(this.params, ((SimpleKey) other).params)));
	}

	@Override
	public final int hashCode() {
		return this.hashCode;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [" + StringUtils.arrayToCommaDelimitedString(this.params) + "]";
	}

}
