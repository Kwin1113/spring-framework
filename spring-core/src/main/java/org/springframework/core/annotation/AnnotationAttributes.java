/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 通过key-value形式存放注解属性的 {@link LinkedHashMap} 子类，该类通过
 * {@link AnnotationUtils}, {@link AnnotatedElementUtils}, 和基于Spring反射
 * 和ASM的注解元数据 {@link org.springframework.core.type.AnnotationMetadata} 的实现解析。
 *
 * 下面这句不太懂：
 * 提供“伪验证”以避免调用代码中嘈杂的Map泛型，以及以类型安全的方式查找注释属性的便捷方法。
 * （大概是说指定了LinkedHashMap的泛型为<String, Object>）
 * <p>Provides 'pseudo-reification' to avoid noisy Map generics in the calling
 * code as well as convenience methods for looking up annotation attributes
 * in a type-safe fashion.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 3.1.1
 * @see AnnotationUtils#getAnnotationAttributes
 * @see AnnotatedElementUtils
 */
@SuppressWarnings("serial")
public class AnnotationAttributes extends LinkedHashMap<String, Object> {

	private static final String UNKNOWN = "unknown";

	@Nullable
	private final Class<? extends Annotation> annotationType;

	final String displayName;

	boolean validated = false;


	/**
	 * 创建一个新的 {@link AnnotationAttributes} 空实例。
	 */
	public AnnotationAttributes() {
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * 创建一个新的 {@link AnnotationAttributes} 空实例，该实例被指定了初始化容量以优化性能。
	 * @param initialCapacity map的初始容量
	 */
	public AnnotationAttributes(int initialCapacity) {
		super(initialCapacity);
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * 通过给定的map和其中的所有键值对创建一个新的 {@link AnnotationAttributes} 实例。
	 * @param map 键值对数据源map
	 * @see #fromMap(Map)
	 */
	public AnnotationAttributes(Map<String, Object> map) {
		super(map);
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * 通过给定的另一个 {@link AnnotationAttributes} 和其键值对创建一个新的
	 * {@link AnnotationAttributes} 实例。
	 * @param other 键值对数据源annotation attributes
	 * @see #fromMap(Map)
	 */
	public AnnotationAttributes(AnnotationAttributes other) {
		super(other);
		this.annotationType = other.annotationType;
		this.displayName = other.displayName;
		this.validated = other.validated;
	}

	/**
	 * 创建一个指定注解类型 {@code annotationType} 的空实例 {@link AnnotationAttributes} 。
	 * @param annotationType 注解类型
	 * {@code AnnotationAttributes} 实例对象; 永不为 {@code null}
	 * @since 4.2
	 */
	public AnnotationAttributes(Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = annotationType;
		this.displayName = annotationType.getName();
	}

	/**
	 * 创建一个可能已被验证的新的空实例 {@link AnnotationAttributes}，指定注解类型为 {@code annotationType}。
	 * @param annotationType 注解类型
	 * {@code AnnotationAttributes} 实例对象; 永不为 {@code null}
	 * @param validated 该注解属性对象是否已被验证
	 * @since 5.2
	 */
	AnnotationAttributes(Class<? extends Annotation> annotationType, boolean validated) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = annotationType;
		this.displayName = annotationType.getName();
		this.validated = validated;
	}

	/**
	 * 创建一个指定 {@code annotationType} 注解类型的新的空实例 {@link AnnotationAttributes} 。
	 * @param annotationType 注解类型
	 * {@code AnnotationAttributes} 实例; 永不为 {@code null}
	 * @param classLoader 给定尝试加载该注解类型的类加载器 ClassLoader，或 {@code null} 表示只存储注解类型的名称
	 * @since 4.3.2
	 */
	public AnnotationAttributes(String annotationType, @Nullable ClassLoader classLoader) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = getAnnotationType(annotationType, classLoader);
		this.displayName = annotationType;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private static Class<? extends Annotation> getAnnotationType(String annotationType, @Nullable ClassLoader classLoader) {
		if (classLoader != null) {
			//尝试使用给定的类加载器加载该注解类
			try {
				return (Class<? extends Annotation>) classLoader.loadClass(annotationType);
			}
			catch (ClassNotFoundException ex) {
				// Annotation Class not resolvable
			}
		}
		return null;
	}


	/**
	 * 获得该注解属性 {@code AnnotationAttributes} 对应的注解类型。
	 * @return 注解类型，如果是未知类型，则返回空 {@code null}
	 * @since 4.2
	 */
	@Nullable
	public Class<? extends Annotation> annotationType() {
		return this.annotationType;
	}

	/**
	 * 按String类型获取特定的属性值 {@code attributeName}。
	 * @param attributeName 获取的属性名称;
	 * 永不为 {@code null} 或空
	 * @return 返回值
	 * @throws IllegalArgumentException 如果属性不存在，或属性不为String类型
	 */
	public String getString(String attributeName) {
		return getRequiredAttribute(attributeName, String.class);
	}

	/**
	 * 按String数组类型获取特定的属性值  {@code attributeName}
	 * 如果该 {@code attributeName} 属性名称对应的是一个String类型值，则在返回前会被包装
	 * 成一个单元素的String数组类型。
	 * @param attributeName 获取的属性名称;
	 * 永不为 {@code null} 或空
	 * @return 返回值
	 * @throws IllegalArgumentException 如果属性不存在，或属性不为String类型或String数组类型
	 */
	public String[] getStringArray(String attributeName) {
		return getRequiredAttribute(attributeName, String[].class);
	}

	/**
	 * 按boolean类型获取特定的属性值  {@code attributeName}
	 * @param attributeName 获取的属性名称;
	 * 永不为 {@code null} 或空
	 * @return 返回值
	 * @throws IllegalArgumentException 如果属性不存在，或属性不为boolean类型
	 */
	public boolean getBoolean(String attributeName) {
		return getRequiredAttribute(attributeName, Boolean.class);
	}

	/**
	 * 按数值Number类型获取特定的属性值  {@code attributeName}
	 * @param attributeName 获取的属性名称;
	 * 永不为 {@code null} 或空
	 * @return 返回值
	 * @throws IllegalArgumentException 如果属性不存在，或属性不为数值Number类型
	 */
	@SuppressWarnings("unchecked")
	public <N extends Number> N getNumber(String attributeName) {
		return (N) getRequiredAttribute(attributeName, Number.class);
	}

	/**
	 * 按枚举enum类型获取特定的属性值  {@code attributeName}
	 * @param attributeName 获取的属性名称;
	 * 永不为 {@code null} 或空
	 * @return 返回值
	 * @throws IllegalArgumentException 如果属性不存在，或属性不为枚举enum类型
	 */
	@SuppressWarnings("unchecked")
	public <E extends Enum<?>> E getEnum(String attributeName) {
		return (E) getRequiredAttribute(attributeName, Enum.class);
	}

	/**
	 * 按Class类型获取特定的属性值  {@code attributeName}
	 * @param attributeName 获取的属性名称;
	 * 永不为 {@code null} 或空
	 * @return 返回值
	 * @throws IllegalArgumentException 如果属性不存在，或属性不为Class类型
	 */
	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> getClass(String attributeName) {
		return getRequiredAttribute(attributeName, Class.class);
	}

	/**
	 * 按Class数组类型获取特定的属性值  {@code attributeName}
	 * 如果该 {@code attributeName} 属性名称对应的是一个Class类型值，则在返回前会被包装
	 * 成一个单元素的Class数组类型。
	 * @param attributeName 获取的属性名称;
	 * 永不为 {@code null} 或空
	 * @return 返回值
	 * @throws IllegalArgumentException 如果属性不存在，或属性不为Class数组类型或Class类型
	 */
	public Class<?>[] getClassArray(String attributeName) {
		return getRequiredAttribute(attributeName, Class[].class);
	}

	/**
	 * 按注解属性 {@link AnnotationAttributes}类型获取特定属性值 {@code attributeName}
	 * 提示：如果想要获取一个注解，请调用 {@link #getAnnotation(String, Class)} 方法。
	 * @param attributeName 获取的属性名称;
	 * 永不为 {@code null} 或空
	 * @return 注解属性 {@code AnnotationAttributes}
	 * @throws IllegalArgumentException 如果属性不存在，或属性不为{@link AnnotationAttributes}类型。
	 */
	public AnnotationAttributes getAnnotation(String attributeName) {
		return getRequiredAttribute(attributeName, AnnotationAttributes.class);
	}

	/**
	 * 按注解类型 {@code annotationType} 获取特定属性值 {@code attributeName}
	 * @param attributeName 获取的属性名称;
	 * 永不为 {@code null} 或空
	 * @param annotationType 期望注解类型; 永不为 {@code null}
	 * @return 对应注解
	 * @throws IllegalArgumentException 如果属性不存在，或属性不为 {@code annotationType}
	 * @since 4.2
	 */
	public <A extends Annotation> A getAnnotation(String attributeName, Class<A> annotationType) {
		return getRequiredAttribute(attributeName, annotationType);
	}

	/**
	 * 按注解属性 {@link AnnotationAttributes} 数组类型获取特定属性值 {@code attributeName}
	 * 如果该 {@code attributeName} 属性名称对应的是一个 {@link AnnotationAttributes} 类型值，则在返回前会被包装
	 * 成一个单元素的 {@link AnnotationAttributes} 数组类型。
	 * 提示：如果想要获取一个注解数组，请调用 {@link #getAnnotationArray(String, Class)} 方法。
	 * @param attributeName 获取的属性名称;
	 * 永不为 {@code null} 或空
	 * @return 注解属性 {@code AnnotationAttributes}
	 * @throws IllegalArgumentException 如果属性不存在，或属性不为 {@link AnnotationAttributes} 类型
	 * 或 {@link AnnotationAttributes} 数组类型。
	 */
	public AnnotationAttributes[] getAnnotationArray(String attributeName) {
		return getRequiredAttribute(attributeName, AnnotationAttributes[].class);
	}

	/**
	 * 按注解数组类型 {@code annotationType} 获取特定属性值 {@code attributeName}
	 * 如果该 {@code attributeName} 属性名称对应的是一个 {@code Annotation} 类型值，则在返回前会被包装
	 * 成一个单元素的 {@code Annotation} 数组类型。
	 * @param attributeName 获取的属性名称;
	 * 永不为 {@code null} 或空
	 * @param annotationType 期望注解类型; 永不为 {@code null}
	 * @return 对应注解数组
	 * @throws IllegalArgumentException 如果属性不存在，或属性不为 {@code annotationType}
	 * 或 {@code annotationType} 数组类型。
	 * @since 4.2
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A[] getAnnotationArray(String attributeName, Class<A> annotationType) {
		Object array = Array.newInstance(annotationType, 0);
		return (A[]) getRequiredAttribute(attributeName, array.getClass());
	}

	/**
	 * 获取保存的特定 {@code attributeName}，保证该值是期望的类型 {@code expectedType}。
	 * 如果  {@code expectedType} 是数组类型，并且该特定的 {@code attributeName} 是单元素
	 * 并且在期望类型数组中，在方法返回之前会把该类型包装成单元素的数组返回。
	 * @param attributeName 获取的属性名称;
	 * 永不为 {@code null} 或空。
	 * @param expectedType 期望返回类型; 永不为 {@code null}
	 * @return 返回值
	 * @throws IllegalArgumentException 如果该属性不存在或该属性不是期望类型
	 */
	@SuppressWarnings("unchecked")
	private <T> T getRequiredAttribute(String attributeName, Class<T> expectedType) {
		Assert.hasText(attributeName, "'attributeName' must not be null or empty");
		//获取属性名称对应的值
		Object value = get(attributeName);
		assertAttributePresence(attributeName, value);
		assertNotException(attributeName, value);
		//如果该值不是传进来的期望类型，判断期望类型是否为数组，并设置成单元素数组
		if (!expectedType.isInstance(value) && expectedType.isArray() &&
				expectedType.getComponentType().isInstance(value)) {
			Object array = Array.newInstance(expectedType.getComponentType(), 1);
			Array.set(array, 0, value);
			value = array;
		}
		assertAttributeType(attributeName, value, expectedType);
		return (T) value;
	}

	private void assertAttributePresence(String attributeName, Object attributeValue) {
		Assert.notNull(attributeValue, () -> String.format(
				"Attribute '%s' not found in attributes for annotation [%s]",
				attributeName, this.displayName));
	}

	private void assertNotException(String attributeName, Object attributeValue) {
		if (attributeValue instanceof Throwable) {
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' for annotation [%s] was not resolvable due to exception [%s]",
					attributeName, this.displayName, attributeValue), (Throwable) attributeValue);
		}
	}

	private void assertAttributeType(String attributeName, Object attributeValue, Class<?> expectedType) {
		if (!expectedType.isInstance(attributeValue)) {
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' is of type %s, but %s was expected in attributes for annotation [%s]",
					attributeName, attributeValue.getClass().getSimpleName(), expectedType.getSimpleName(),
					this.displayName));
		}
	}

	@Override
	public String toString() {
		Iterator<Map.Entry<String, Object>> entries = entrySet().iterator();
		StringBuilder sb = new StringBuilder("{");
		while (entries.hasNext()) {
			Map.Entry<String, Object> entry = entries.next();
			sb.append(entry.getKey());
			sb.append('=');
			sb.append(valueToString(entry.getValue()));
			sb.append(entries.hasNext() ? ", " : "");
		}
		sb.append("}");
		return sb.toString();
	}

	private String valueToString(Object value) {
		if (value == this) {
			return "(this Map)";
		}
		if (value instanceof Object[]) {
			return "[" + StringUtils.arrayToDelimitedString((Object[]) value, ", ") + "]";
		}
		return String.valueOf(value);
	}


	/**
	 * 通过给定的map返回一个 {@link AnnotationAttributes}。
	 * 如果这个map已经存在一个对应的 {@code AnnotationAttributes} 实例，该map会被强制转型
	 * 并直接返回，不创建新的 {@code AnnotationAttributes} 实例。
	 * 否则将通过 {@link #AnnotationAttributes(Map)} 构造器来创建一个新实例。
	 * @param map 注解属性map键值对元数据
	 */
	@Nullable
	public static AnnotationAttributes fromMap(@Nullable Map<String, Object> map) {
		if (map == null) {
			return null;
		}
		if (map instanceof AnnotationAttributes) {
			return (AnnotationAttributes) map;
		}
		return new AnnotationAttributes(map);
	}

}
