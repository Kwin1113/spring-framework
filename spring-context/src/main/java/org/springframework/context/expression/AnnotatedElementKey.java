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

package org.springframework.context.expression;

import java.lang.reflect.AnnotatedElement;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 表示一个特定类上的带注解的元素{@link AnnotatedElement}，可以作为key来使用
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @since 4.2
 * @see CachedExpressionEvaluator
 */
public final class AnnotatedElementKey implements Comparable<AnnotatedElementKey> {

	private final AnnotatedElement element;

	@Nullable
	private final Class<?> targetClass;


	/**
	 * 通过指定的{@link AnnotatedElement}和可选的目标类{@link Class}创建一个实例。
	 */
	public AnnotatedElementKey(AnnotatedElement element, @Nullable Class<?> targetClass) {
		Assert.notNull(element, "AnnotatedElement must not be null");
		this.element = element;
		this.targetClass = targetClass;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AnnotatedElementKey)) {
			return false;
		}
		AnnotatedElementKey otherKey = (AnnotatedElementKey) other;
		return (this.element.equals(otherKey.element) &&
				ObjectUtils.nullSafeEquals(this.targetClass, otherKey.targetClass));
	}

	@Override
	public int hashCode() {
		return this.element.hashCode() + (this.targetClass != null ? this.targetClass.hashCode() * 29 : 0);
	}

	@Override
	public String toString() {
		return this.element + (this.targetClass != null ? " on " + this.targetClass : "");
	}

	@Override
	public int compareTo(AnnotatedElementKey other) {
		int result = this.element.toString().compareTo(other.element.toString());
		if (result == 0 && this.targetClass != null) {
			if (other.targetClass == null) {
				return 1;
			}
			result = this.targetClass.getName().compareTo(other.targetClass.getName());
		}
		return result;
	}

}
