/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context.annotation.aspectj;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * 指示当前应用程序上下文将依赖项注入应用于在Spring bean工厂外部实例化的非托管类
 * (通常为注解了
 * {@link org.springframework.beans.factory.annotation.Configurable @Configurable}
 * 注解的类).
 *
 * 类似于Spring{@code <context:spring-configured>} XML元素的功能
 * 通常结合
 * {@link org.springframework.context.annotation.EnableLoadTimeWeaving @EnableLoadTimeWeaving}
 * 使用
 *
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.context.annotation.EnableLoadTimeWeaving
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SpringConfiguredConfiguration.class)
public @interface EnableSpringConfigured {

}
