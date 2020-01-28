/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.lang.Nullable;

/**
 * {@link DefaultBeanDefinitionDocumentReader}用于处理自定义顶级
 * （直接在{@code <beans />}下）标记的接口。
 *
 * <p>实现类可以轻松的实现把自定义tag中的元数据转换成所需数量的
 * {@link BeanDefinition BeanDefinitions}
 *
 * <p>解析起会从自定义tag所在的关联命名空间中定位到{@link BeanDefinitionParser}
 *
 * @author Rob Harrop
 * @since 2.0
 * @see NamespaceHandler
 * @see AbstractBeanDefinitionParser
 */
public interface BeanDefinitionParser {

	/**
	 * 解析给定的{@link Element}，并且将所得的{@link BeanDefinition BeanDefinition(s)}
	 * 通过提供的{@link ParserContext}中的
	 * {@link org.springframework.beans.factory.xml.ParserContext#getRegistry() BeanDefinitionRegistry}
	 * 方法注册到其中
	 * <p>如果实现类将以嵌套方式使用（例如，作为{@code <property />}标记中的内部标记），
	 * 则实现必须返回解析产生的主要{@link BeanDefinition}。
	 * 当实现类不以嵌套方式使用时，方法可能返回{@code null}
	 * @param element 给定的元素，可能被解析为一个或多个{@link BeanDefinition BeanDefinitions}
	 * @param parserContext 封装当前解析进程的对象；提供
	 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}的连接
	 * @return 主要的{@link BeanDefinition}
	 */
	@Nullable
	BeanDefinition parse(Element element, ParserContext parserContext);

}
