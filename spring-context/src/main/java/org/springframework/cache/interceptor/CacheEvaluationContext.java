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

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * 缓存特定求值上下文，以懒汉方式将方法参数添加为SpEL变量
 * 懒加载消除了参数发现时不必要的类字节码的解析
 * <p>
 * 还定义了一组“不可用变量”（即，在访问变量时应导致异常的变量）。
 * 这对于验证条件不满足时（即使不是所有潜在变量都存在时）很有用。
 * <p>
 * 为了限制对象的创建，提供了一个很‘丑陋/简陋’的构造器
 * （不是专门的闭包类-用于延迟加载）
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 */
class CacheEvaluationContext extends MethodBasedEvaluationContext {

	private final Set<String> unavailableVariables = new HashSet<>(1);


	CacheEvaluationContext(Object rootObject, Method method, Object[] arguments,
						   ParameterNameDiscoverer parameterNameDiscoverer) {

		super(rootObject, method, arguments, parameterNameDiscoverer);
	}


	/**
	 * 将指定的变量名称添加为上下文不可用。
	 * 任何尝试使用该变量的表达式都将引起异常。
	 * 可以验证可能存在变量的表达式，即使该变量不可用。因此所有尝试使用该变量的表达式都会
	 * 求值失败。
	 */
	public void addUnavailableVariable(String name) {
		this.unavailableVariables.add(name);
	}


	/**
	 * 只在需要时加载参数信息
	 */
	@Override
	@Nullable
	public Object lookupVariable(String name) {
		if (this.unavailableVariables.contains(name)) {
			throw new VariableNotAvailableException(name);
		}
		return super.lookupVariable(name);
	}

}
