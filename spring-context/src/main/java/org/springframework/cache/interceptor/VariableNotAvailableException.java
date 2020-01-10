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

import org.springframework.expression.EvaluationException;

/**
 * 特定的求值异常{@link EvaluationException}，提示无法获取缓存上下文中表达式所给的某个变量。
 *
 * @author Stephane Nicoll
 * @since 4.0.6
 */
@SuppressWarnings("serial")
class VariableNotAvailableException extends EvaluationException {

	private final String name;


	public VariableNotAvailableException(String name) {
		super("Variable not available");
		this.name = name;
	}


	public final String getName() {
		return this.name;
	}

}
