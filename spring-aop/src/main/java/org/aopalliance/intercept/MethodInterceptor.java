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

package org.aopalliance.intercept;

/**
 * 方法级别的拦截器
 *
 * 在通过接口调用目标对象的过程中拦截调用。嵌套在目标对象的"上面"
 *
 * 用户需要实现{@link #invoke(MethodInvocation)}方法，来修改原始的方法行为表现。
 * 例如：下列类实现追踪拦截器（追踪拦截方法的所有调用）：
 *
 * <pre class=code>
 * class TracingInterceptor implements MethodInterceptor {
 *   Object invoke(MethodInvocation i) throws Throwable {
 *     System.out.println("method "+i.getMethod()+" is called on "+
 *                        i.getThis()+" with args "+i.getArguments());
 *     Object ret=i.proceed();
 *     System.out.println("method "+i.getMethod()+" returns "+ret);
 *     return ret;
 *   }
 * }
 * </pre>
 *
 * @author Rod Johnson
 */
@FunctionalInterface
public interface MethodInterceptor extends Interceptor {

	/**
	 * 实现该方法，以在调用前后执行额外的操作
	 * 合理的实现方式应该调用{@link Joinpoint#proceed()}方法
	 *
	 * @param invocation 方法调用的切入点
	 * @return {@link Joinpoint#proceed()}调用的结果;
	 * 可能会被拦截器拦截
	 * @throws Throwable 拦截器或目标对象抛出的异常
	 */
	Object invoke(MethodInvocation invocation) throws Throwable;

}
