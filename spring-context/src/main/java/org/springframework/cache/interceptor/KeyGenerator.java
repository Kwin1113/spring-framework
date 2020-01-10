/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.reflect.Method;

/**
 * 缓存key生成器。基于给定方法（缓存上下文中）和参数生成key。
 *
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.1
 */
@FunctionalInterface
public interface KeyGenerator {

	/**
	 * 通过给定方法和参数生成一个key。
	 * @param target 目标实例
	 * @param method 被调用的方法
	 * @param params 方法参数(with any var-args expanded)
	 * @return 生成的key
	 */
	Object generate(Object target, Method method, Object... params);

}
