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

package org.springframework.cache.annotation;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.*;

/**
 * 开启Spring的注解驱动功能，类似Spring XML命名空间的配置。
 * 配合@{@link org.springframework.context.annotation.Configuration Configuration}一起使用：
 *
 * <pre class="code">
 * @ Configuration
 * @ EnableCaching
 * public class AppConfig {
 *
 *     @ Bean
 *     public MyService myService() {
 *         // configure and return a class having @ Cacheable methods
 *         return new MyService();
 *     }
 *
 *     @ Bean
 *     public CacheManager cacheManager() {
 *         // configure and return an implementation of Spring's CacheManager SPI
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 * }</pre>
 * <p>
 * 和以下Spring XML配置文件相比：
 *
 * <pre class="code">
 * < beans>
 *
 *     < cache:annotation-driven/>;
 *
 *     < bean id="myService" class="com.foo.MyService"/>;
 *
 *     < bean id="cacheManager" class="org.springframework.cache.support.SimpleCacheManager">
 *         < property name="caches">
 *             < set>
 *                 < bean class="org.springframework.cache.concurrent.ConcurrentMapCacheFactoryBean">
 *                     < property name="name" value="default"/>
 *                 < /bean>
 *             < /set>
 *         < /property>
 *     < /bean>
 *
 * < /beans>
 * </pre>
 * <p>
 * 在以上的两种场景中 {@code @EnableCaching} 和 {@code < cache:annotation-driven/>} 都负责注册
 * Spring中注解驱动缓存管理的必要组件，
 * 比如 {@link org.springframework.cache.interceptor.CacheInterceptor CacheInterceptor}
 * 和调用 {@link org.springframework.cache.annotation.Cacheable @Cacheable}
 * 时通过基于代理或AspectJ的增强将拦截器编织到调用堆栈中。
 * <p>
 * 如果存在 JSR-107 API和Spring JCache 实现，则还将注册用于管理标准缓存注解的必要组件。
 * 这会在被 {@code CacheResult}, {@code CachePut}, {@code CacheRemove} 或
 * {@code CacheRemoveAll} 注解的方法被调用时创建基于代理或AspectJ的增强，编制到调用堆栈中。
 * <p>
 * 当框架没有一个合理的默认值可以使用的时候，会创建一个类型为
 * {@link org.springframework.cache.CacheManager CacheManager} 的bean。
 * {@code @EnableCaching} 通过类型 by type 寻找项目中的缓存管理器，而 {@code <cache:annotation-driven>}
 * 则通过名称 named "cacheManager"来寻找。因此缓存管理器的命名就不是很重要了。
 * <p>
 * 对于想要在 {@code @EnableCaching} 和确切的缓存管理器之间建立更直接关系的用户，
 * 可以实现 {@link CachingConfigurer} 回调接口。
 * 注意看一下被 {@code @Override} 标记的方法：
 *
 * <pre class="code">
 * @ Configuration
 * @ EnableCaching
 * public class AppConfig extends CachingConfigurerSupport {
 *
 *     @ Bean
 *     public MyService myService() {
 *         // configure and return a class having @ Cacheable methods
 *         return new MyService();
 *     }
 *
 *     @ Bean
 *     @ Override
 *     public CacheManager cacheManager() {
 *         // configure and return an implementation of Spring's CacheManager SPI
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 *
 *     @ Bean
 *     @ Override
 *     public KeyGenerator keyGenerator() {
 *         // configure and return an implementation of Spring's KeyGenerator SPI
 *         return new MyKeyGenerator();
 *     }
 * }</pre>
 * <p>
 * 这种方式简单可取，因为它是显式的，或者说如果需要区分一个容器中两个 {@code CacheManager} 的时候
 * 是必须这么做的。注意以上例子中 {@code keyGenerator} 方法也被重写。这个方法允许通过
 * Spring's {@link org.springframework.cache.interceptor.KeyGenerator KeyGenerator} SPI
 * 自定义缓存key生成策略。一般情况下， {@code @EnableCaching} 会配置一个默认的
 * {@link org.springframework.cache.interceptor.SimpleKeyGenerator SimpleKeyGenerator}
 * 生成策略，但是实现了 {@code CachingConfigurer} 的话，必须显式地提供一个key生成器。
 * 如果不需要自定义的话，可以返回 {@code null} 或 {@code new SimpleKeyGenerator()}。
 * <p>
 * {@link CachingConfigurer} 提供额外的自定义选项：
 * 推荐使用继承 {@link org.springframework.cache.annotation.CachingConfigurerSupport
 * CachingConfigurerSupport} 的方式自定义，该类提供了接口所有方法的默认实现，因此只需要重写
 * 需要的方法即可，无需实现所有接口方法。查看 {@link CachingConfigurer} 的Javadoc获取更多细节。
 * <p>
 * {@link #mode} 属性控制增强如何实现：如果该参数为 {@link AdviceMode#PROXY} （默认参数），
 * 那么其他参数将通过代理的方式实现。请注意，代理模式只允许拦截器通过代理调用；同一个类里的
 * 本地调用无法以这种方式被拦截。
 * <p>
 * 注意，如果 {@linkplain #mode} 被设置为 {@link AdviceMode#ASPECTJ}，那么
 * {@link #proxyTargetClass} 属性将会被忽略。这种情况下 {@code spring-aspects} 模块JAR
 * 必须被引入classpath，通过编译时织入或加载时织入将切面织入到目标类中。该模式场景下
 * 没有代理调用的发生；本地调用也会被拦截。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see CachingConfigurer
 * @see CachingConfigurationSelector
 * @see ProxyCachingConfiguration
 * @see org.springframework.cache.aspectj.AspectJCachingConfiguration
 * @since 3.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CachingConfigurationSelector.class)
public @interface EnableCaching {

	/**
	 * 指明是否不创建标准Java基于接口的代理（JDK），而创建基于子类的代理（CGLIB）。
	 * 默认为 {@code false}。仅在 {@link #mode()} 为 {@link AdviceMode#PROXY} 的情况下
	 * 适用。注意，设置该属性为 {@code true} 将会影响所有Spring管理下需要代理的bean，
	 * 不仅仅只影响标记了 {@code @Cacheable} 注解的接口。
	 * 例如，其他标记了Spring {@code @Transactional} 注解的bean也会同时更新为CGLIB代理。
	 * 该行为实际上没有任何负面影响，除非某个bean显式地指定需要使用另一种代理模式。
	 */
	boolean proxyTargetClass() default false;

	/**
	 * 指明缓存增强如何实现。
	 * 默认为 {@link AdviceMode#PROXY}。
	 * 请注意，代理模式只允许拦截器通过代理调用；同一个类里的本地调用无法以这种方式被拦截；
	 * 本地调用方法时方法上的缓存注解将被忽略，因为Spring拦截器在运行时根本没有起作用。
	 * 如果需要更高级的拦截器模式，考虑换成 {@link AdviceMode#ASPECTJ}。
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * 指明多个增强同时应用于一个切入点joinpoint时，缓存增强的执行顺序。
	 * 默认为 {@link Ordered#LOWEST_PRECEDENCE}，优先级最低。
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
