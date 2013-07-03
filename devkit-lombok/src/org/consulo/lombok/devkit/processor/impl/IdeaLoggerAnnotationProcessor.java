/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.consulo.lombok.devkit.processor.impl;

import org.consulo.lombok.processors.impl.AbstractLoggerAnnotationProcessor;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 20:08/03.06.13
 */
public class IdeaLoggerAnnotationProcessor extends AbstractLoggerAnnotationProcessor {
  public IdeaLoggerAnnotationProcessor(String annotationClass) {
    super(annotationClass);
  }

  @NotNull
  @Override
  protected String getFieldName() {
    return "LOGGER";
  }

  @Override
  public String getLoggerClass() {
    return "com.intellij.openapi.diagnostic.Logger";
  }
}
