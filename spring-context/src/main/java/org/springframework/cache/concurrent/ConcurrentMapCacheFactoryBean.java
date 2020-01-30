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

package org.springframework.cache.concurrent;

import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 当在Spring容器中使用时，可以通过该{@link FactoryBean}对{@link ConcurrentMapCache}
 * 进行简单的配置。可以通过bean属性进行配置； 使用分配的Spring bean名称作为默认缓存名称。
 *
 * 在测试或简单的缓存场景中很有用，通常与{@link org.springframework.cache.support.SimpleCacheManager}
 * 配合使用，或者通过{@link ConcurrentMapCacheManager}动态使用。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ConcurrentMapCacheFactoryBean
		implements FactoryBean<ConcurrentMapCache>, BeanNameAware, InitializingBean {

	private String name = "";

	@Nullable
	private ConcurrentMap<Object, Object> store;

	private boolean allowNullValues = true;

	@Nullable
	private ConcurrentMapCache cache;


	/**
	 * Specify the name of the cache.
	 * <p>Default is "" (empty String).
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Specify the ConcurrentMap to use as an internal store
	 * (possibly pre-populated).
	 * <p>Default is a standard {@link java.util.concurrent.ConcurrentHashMap}.
	 */
	public void setStore(ConcurrentMap<Object, Object> store) {
		this.store = store;
	}

	/**
	 * Set whether to allow {@code null} values
	 * (adapting them to an internal null holder value).
	 * <p>Default is "true".
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		this.allowNullValues = allowNullValues;
	}

	@Override
	public void setBeanName(String beanName) {
		if (!StringUtils.hasLength(this.name)) {
			setName(beanName);
		}
	}

	@Override
	public void afterPropertiesSet() {
		this.cache = (this.store != null ? new ConcurrentMapCache(this.name, this.store, this.allowNullValues) :
				new ConcurrentMapCache(this.name, this.allowNullValues));
	}


	@Override
	@Nullable
	public ConcurrentMapCache getObject() {
		return this.cache;
	}

	@Override
	public Class<?> getObjectType() {
		return ConcurrentMapCache.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
