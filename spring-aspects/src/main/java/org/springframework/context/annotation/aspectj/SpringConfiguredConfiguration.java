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

package org.springframework.context.annotation.aspectj;

import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * {@code @Configuration}配置类，用于注册{@code AnnotationBeanConfigurerAspect}，以支持
 * 非Spring托管的对象（带有@{@link org.springframework.beans.factory.annotation.Configurable
 * Configurable}注解）
 *
 * 该配置类在使用{@link EnableSpringConfigured @EnableSpringConfigured}时会
 * 自动被引用
 * See {@code @EnableSpringConfigured}'s javadoc for complete usage details.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableSpringConfigured
 */
@Configuration
public class SpringConfiguredConfiguration {

	/**
	 * 配置切面使用的bean名称
	 */
	public static final String BEAN_CONFIGURER_ASPECT_BEAN_NAME =
			"org.springframework.context.config.internalBeanConfigurerAspect";

	@Bean(name = BEAN_CONFIGURER_ASPECT_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public AnnotationBeanConfigurerAspect beanConfigurerAspect() {
		return AnnotationBeanConfigurerAspect.aspectOf();
	}

}
