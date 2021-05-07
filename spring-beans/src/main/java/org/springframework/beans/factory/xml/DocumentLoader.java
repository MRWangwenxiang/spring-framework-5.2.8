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

package org.springframework.beans.factory.xml;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

/**
 * Strategy interface for loading an XML {@link Document}.
 *
 * @author Rob Harrop
 * @see DefaultDocumentLoader
 * @since 2.0
 */
public interface DocumentLoader {

	/**
	 * 将提供的{@link InputSource inputSource}加载为{@link Document document}
	 *
	 * @param inputSource    要加载的文档的源
	 * @param entityResolver 用于解析实体的解析器
	 * @param errorHandler   文档加载过程中的任何错误的钩子处理类
	 * @param validationMode 验证的类型{@link org.springframework.util.xml.XmlValidationModeDetector#VALIDATION_DTD DTD}或{@link org.springframework.util.xml.XmlValidationModeDetector#VALIDATION_XSD XSD})
	 * @param namespaceAware 如果要提供对XML名称空间的支持，则为{@code true}
	 * @return 加载完成的{@link Document document}
	 * @throws Exception 加载过程中发生的错误
	 */
	Document loadDocument(
			InputSource inputSource, EntityResolver entityResolver,
			ErrorHandler errorHandler, int validationMode, boolean namespaceAware)
			throws Exception;
}
