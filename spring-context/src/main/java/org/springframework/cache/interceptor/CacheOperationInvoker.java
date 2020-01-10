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

/**
 * 抽象化缓存操作的调用。
 * <p>
 * 不提供传输检查型异常checked exceptions的方法，而是提供一种特殊的异常，
 * 该异常应用于包装由基础调用引发的任何异常。
 * 调用者应该特殊处理此类异常。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@FunctionalInterface
public interface CacheOperationInvoker {

	/**
	 * 调用实例定义的缓存操作。所有调用期间抛出的异常都会被{@link ThrowableWrapper}包装。
	 *
	 * @return 操作的结果
	 * @throws ThrowableWrapper 调用操作过程中发生了错误
	 */
	Object invoke() throws ThrowableWrapper;


	/**
	 * 包装所有调用{@link #invoke()}过程中抛出的异常。
	 */
	@SuppressWarnings("serial")
	class ThrowableWrapper extends RuntimeException {

		private final Throwable original;

		public ThrowableWrapper(Throwable original) {
			super(original.getMessage(), original);
			this.original = original;
		}

		public Throwable getOriginal() {
			return this.original;
		}
	}

}
