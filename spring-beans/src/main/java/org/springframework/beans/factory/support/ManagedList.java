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

package org.springframework.beans.factory.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;
import org.springframework.lang.Nullable;

/**
 * 标签tag集合类，用于管理可能持有运行时runtime bean引用的List元素（将被解析成bean对象）
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 27.05.2003
 * @param <E> the element type
 */
@SuppressWarnings("serial")
public class ManagedList<E> extends ArrayList<E> implements Mergeable, BeanMetadataElement {

	@Nullable
	private Object source;

	@Nullable
	private String elementTypeName;

	private boolean mergeEnabled;


	public ManagedList() {
	}

	public ManagedList(int initialCapacity) {
		super(initialCapacity);
	}


	/**
	 * 给该元数据元素设置配置源对象{@code Object}
	 * 对象的确切类型取决于使用的配置机制
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

	/**
	 * 设置默认的元素类型名称（类名称），用于该list
	 */
	public void setElementTypeName(String elementTypeName) {
		this.elementTypeName = elementTypeName;
	}

	/**
	 * 返回该list的默认元素类型名称（类名称）
	 */
	@Nullable
	public String getElementTypeName() {
		return this.elementTypeName;
	}

	/**
	 * 设置该集合存在父集合的情况下是否允许合并
	 */
	public void setMergeEnabled(boolean mergeEnabled) {
		this.mergeEnabled = mergeEnabled;
	}

	@Override
	public boolean isMergeEnabled() {
		return this.mergeEnabled;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<E> merge(@Nullable Object parent) {
		//不允许合并
		if (!this.mergeEnabled) {
			throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
		}
		//没有父集合，直接返回自身
		if (parent == null) {
			return this;
		}
		//父集合不是集合类型
		if (!(parent instanceof List)) {
			throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
		}
		List<E> merged = new ManagedList<>();
		merged.addAll((List<E>) parent);
		merged.addAll(this);
		return merged;
	}

}
