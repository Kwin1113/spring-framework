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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 简单的{@link CacheOperationSource}实现，允许属性通过注册名称进行匹配
 *
 * @author Costin Leau
 * @since 3.1
 */
@SuppressWarnings("serial")
public class NameMatchCacheOperationSource implements CacheOperationSource, Serializable {

	/**
	 * 子类可继承的日志Logger
	 * static静态在序列化时进行优化
	 */
	protected static final Log logger = LogFactory.getLog(NameMatchCacheOperationSource.class);


	/**
	 * Keys->方法名称
	 * Values->事务属性
	 * Keys are method names; values are TransactionAttributes.
	 */
	private Map<String, Collection<CacheOperation>> nameMap = new LinkedHashMap<>();


	/**
	 * 设置名称/属性map
	 * 该map包含方法名称(e.g. "myMethod")
	 * 和缓存操作实例CacheOperation instances
	 * （or Strings to be converted to CacheOperation instances）
	 *
	 * @see CacheOperation
	 */
	public void setNameMap(Map<String, Collection<CacheOperation>> nameMap) {
		nameMap.forEach(this::addCacheMethod);
	}

	/**
	 * 为可缓存的方法增加一个属性
	 * 方法名称可以精确匹配，也可以通过格式"xxx*","*xxx" 或 "*xxx*"匹配多个方法。
	 *
	 * @param methodName 方法名称
	 * @param ops        方法相关的操作
	 */
	public void addCacheMethod(String methodName, Collection<CacheOperation> ops) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding method [" + methodName + "] with cache operations [" + ops + "]");
		}
		this.nameMap.put(methodName, ops);
	}

	@Override
	@Nullable
	public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
		// 查询名称匹配的缓存操作
		String methodName = method.getName();
		Collection<CacheOperation> ops = this.nameMap.get(methodName);

		if (ops == null) {
			// 匹配特定格式的缓存操作
			String bestNameMatch = null;
			for (String mappedName : this.nameMap.keySet()) {
				if (isMatch(methodName, mappedName)
						&& (bestNameMatch == null || bestNameMatch.length() <= mappedName.length())) {
					ops = this.nameMap.get(mappedName);
					bestNameMatch = mappedName;
				}
			}
		}

		return ops;
	}

	/**
	 * 返回给定方法名称是否满足给定格式。
	 * 默认实现会检查"xxx*", "*xxx", "*xxx*"匹配项和直接相等的名称。
	 * 可被子类重写
	 *
	 * @param methodName 类中的方法名
	 * @param mappedName 描述器中的缓存名称
	 * @return 名称是否匹配
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NameMatchCacheOperationSource)) {
			return false;
		}
		NameMatchCacheOperationSource otherTas = (NameMatchCacheOperationSource) other;
		return ObjectUtils.nullSafeEquals(this.nameMap, otherTas.nameMap);
	}

	@Override
	public int hashCode() {
		return NameMatchCacheOperationSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.nameMap;
	}
}
