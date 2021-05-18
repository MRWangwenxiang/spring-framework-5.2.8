/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;

import java.io.IOException;

/**
 * {@link org.springframework.context.ApplicationContext}的一个实现基类。
 * <p>
 * 该类支持对{@link #refresh()}多次调用， 每次调用都将创建一个新的内部beanFactory实例，典型的模板模式。
 * <p>
 * 子类唯一需要实现的方法是{@link #loadBeanDefinitions}，每次调用{@link #refresh()}刷新时都会调用该方法。
 * <p>
 * {@link #loadBeanDefinitions}其具体的实现逻辑应该是将beanDefinitions加载到给定的{@link org.springframework.beans.factory.support.DefaultListableBeanFactory}中，通常委托给一个或多个特定的beanDefinition读取器，典型的委派模式。
 * <p>
 * 注意，WebApplicationContexts有一个类似的基类。
 * {@link org.springframework.web.context.support.AbstractRefreshableWebApplicationContext}提供了相同的子类化策略，
 * 但是还预实现了Web环境的所有上下文功能。
 * 还有一种预定义的方式来接收Web上下文的配置位置。
 * <p>
 * 读取beanDefinition的两种方式一种是xml方式，该方式提供了通用的{@link AbstractXmlApplicationContext}基类，从基类根据不同读取方式派生出
 * {@link ClassPathXmlApplicationContext}和{@link FileSystemXmlApplicationContext}。
 * <p>
 * 另一种
 * {@link org.springframework.context.annotation.AnnotationConfigApplicationContext}支持{@code @Configuration}注解的类作为beanDefinition的源
 * <p>
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see #loadBeanDefinitions
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.web.context.support.AbstractRefreshableWebApplicationContext
 * @see AbstractXmlApplicationContext
 * @see ClassPathXmlApplicationContext
 * @see FileSystemXmlApplicationContext
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 * @since 1.1.3
 */
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

	@Nullable
	private Boolean allowBeanDefinitionOverriding;

	@Nullable
	private Boolean allowCircularReferences;

	/**
	 * 该上下文的beanFactory
	 */
	// TODO 为什么要用volatile来修饰
	@Nullable
	private volatile DefaultListableBeanFactory beanFactory;


	/**
	 * Create a new AbstractRefreshableApplicationContext with no parent.
	 */
	public AbstractRefreshableApplicationContext() {
	}

	/**
	 * Create a new AbstractRefreshableApplicationContext with the given parent context.
	 *
	 * @param parent the parent context
	 */
	public AbstractRefreshableApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * 设置是否允许覆盖beanDefinition，默认值为{@code true}
	 * <p>
	 * 允许覆盖:注册具有相同名称的另一个定义将自动替换前一个定义。
	 * 不允许覆盖:将引发异常。
	 * <p>
	 * 实际是将该属性通过{@linkplain #customizeBeanFactory(DefaultListableBeanFactory)  "customizeBeanFactory"}设置给了
	 * {@linkplain org.springframework.beans.factory.support.DefaultListableBeanFactory}
	 *
	 * @param allowBeanDefinitionOverriding
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * 设置是否允许bean之间的循环引用，并自动尝试解决它们，默认值为{@code true}
	 * <p>
	 * 允许循环引用：会自动尝试解决它们
	 * 不允许循环引用:用引发异常
	 * <p>
	 * 实际是将该属性通过{@linkplain #customizeBeanFactory(DefaultListableBeanFactory)  "customizeBeanFactory"}设置给了
	 * {@linkplain org.springframework.beans.factory.support.DefaultListableBeanFactory}
	 *
	 * @param allowCircularReferences
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 */
	// TODO	 并不清楚循环引用怎么自动解决
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.allowCircularReferences = allowCircularReferences;
	}


	/**
	 * 该类最重要的方法
	 * <p>
	 * 实现对该上下文的beanFactory进行刷新
	 * <p>
	 * 如果已经有一个beanFactory则关闭，为该上下文初始化一个新的beanFactory
	 *
	 * @throws BeansException
	 */
	@Override
	protected final void refreshBeanFactory() throws BeansException {
		// 1、第一步判断是否有beanFactory，如果有则关闭beanFactory
		if (hasBeanFactory()) {
			// TODO 如何释放掉bean
			destroyBeans();
			// TODO 关闭bean工厂，为什么只是清理了实例变量，里面逻辑没看懂
			closeBeanFactory();
		}
		try {
			// 2、第二部创建一个新的beanFactory
			// TODO 如何创建一个 新的factory
			DefaultListableBeanFactory beanFactory = createBeanFactory();
			// TODO beanFactory里的ID有什么特殊用途
			beanFactory.setSerializationId(getId());
			// 定制beanFactory属性，override和circularReferences属性
			customizeBeanFactory(beanFactory);
			// 重点方法*****: 加载beanDefinitions
			loadBeanDefinitions(beanFactory);
			this.beanFactory = beanFactory;
		} catch (IOException ex) {
			throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
		}
	}

	@Override
	protected void cancelRefresh(BeansException ex) {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory != null) {
			beanFactory.setSerializationId(null);
		}
		super.cancelRefresh(ex);
	}

	@Override
	protected final void closeBeanFactory() {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory != null) {
			beanFactory.setSerializationId(null);
			this.beanFactory = null;
		}
	}

	/**
	 * 确定此上下文当前是否拥有Bean工厂
	 * <p>
	 * 即是否至少刷新一次且尚未关闭
	 *
	 * @return
	 */
	protected final boolean hasBeanFactory() {
		return (this.beanFactory != null);
	}

	@Override
	public final ConfigurableListableBeanFactory getBeanFactory() {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory == null) {
			throw new IllegalStateException("BeanFactory not initialized or already closed - " +
					"call 'refresh' before accessing beans via the ApplicationContext");
		}
		return beanFactory;
	}

	/**
	 * Overridden to turn it into a no-op: With AbstractRefreshableApplicationContext,
	 * {@link #getBeanFactory()} serves a strong assertion for an active context anyway.
	 */
	@Override
	protected void assertBeanFactoryActive() {
	}

	/**
	 * Create an internal bean factory for this context.
	 * Called for each {@link #refresh()} attempt.
	 * <p>The default implementation creates a
	 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}
	 * with the {@linkplain #getInternalParentBeanFactory() internal bean factory} of this
	 * context's parent as parent bean factory. Can be overridden in subclasses,
	 * for example to customize DefaultListableBeanFactory's settings.
	 *
	 * @return the bean factory for this context
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 */
	protected DefaultListableBeanFactory createBeanFactory() {
		return new DefaultListableBeanFactory(getInternalParentBeanFactory());
	}

	/**
	 * Customize the internal bean factory used by this context.
	 * Called for each {@link #refresh()} attempt.
	 * <p>The default implementation applies this context's
	 * {@linkplain #setAllowBeanDefinitionOverriding "allowBeanDefinitionOverriding"}
	 * and {@linkplain #setAllowCircularReferences "allowCircularReferences"} settings,
	 * if specified. Can be overridden in subclasses to customize any of
	 * {@link DefaultListableBeanFactory}'s settings.
	 *
	 * @param beanFactory the newly created bean factory for this context
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see DefaultListableBeanFactory#setAllowCircularReferences
	 * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 * @see DefaultListableBeanFactory#setAllowEagerClassLoading
	 */
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
		if (this.allowBeanDefinitionOverriding != null) {
			beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
		}
		if (this.allowCircularReferences != null) {
			beanFactory.setAllowCircularReferences(this.allowCircularReferences);
		}
	}


	/**
	 * 通常通过委派一个或多个bean定义读取器，将beanDefinitions加载到给定的beanFactory中
	 * <p>
	 * 会有多种实现:例如xml和annotation
	 *
	 * @param beanFactory 将beanDefinitions加载到的beanFactory
	 * @throws BeansException 如果解析bean定义失败
	 * @throws IOException    如果加载bean定义文件失败
	 * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
			throws BeansException, IOException;

}
