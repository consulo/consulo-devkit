// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.idea.devkit.dom;

import com.intellij.java.impl.util.xml.ExtendClass;
import com.intellij.java.language.psi.PsiClass;
import consulo.xml.util.xml.*;
import org.jetbrains.idea.devkit.dom.impl.PluginPsiClassConverter;

import jakarta.annotation.Nonnull;
import java.util.List;

public interface Listeners extends DomElement {

  @Nonnull
  @SubTagList("listener")
  List<Listener> getListeners();


  interface Listener extends DomElement {

    @Attribute("class")
    @Required
    @Convert(PluginPsiClassConverter.class)
    @ExtendClass(allowNonPublic = true, allowAbstract = false, allowInterface = false, allowEnum = false)
    GenericAttributeValue<PsiClass> getListenerClassName();

    @Attribute("topic")
    @Required
    @Convert(PluginPsiClassConverter.class)
    @ExtendClass(allowNonPublic = true, allowEnum = false, instantiatable = false)
    GenericAttributeValue<PsiClass> getTopicClassName();

    @Attribute("activeInTestMode")
    GenericAttributeValue<Boolean> isActiveInTestMode();

    @Attribute("activeInHeadlessMode")
    GenericAttributeValue<Boolean> isActiveInHeadlessMode();
  }
}
