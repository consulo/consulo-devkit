/*
 * Copyright 2011-present Greg Shrago
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
package org.intellij.grammar.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.fileEditor.structureView.tree.SortableTreeElement;
import consulo.language.Language;
import consulo.language.editor.structureView.PsiStructureViewFactory;
import consulo.language.editor.structureView.PsiTreeElementBase;
import consulo.language.editor.structureView.StructureViewModelBase;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.grammar.BnfLanguage;
import org.intellij.grammar.psi.*;
import org.intellij.grammar.psi.impl.BnfFileImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author gregsh
 */
@ExtensionImpl
public class BnfStructureViewFactory implements PsiStructureViewFactory {
    @Override
    public StructureViewBuilder getStructureViewBuilder(final PsiFile psiFile) {
        return new TreeBasedStructureViewBuilder() {
            @Nonnull
            @Override
            public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
                return new MyModel(psiFile);
            }

            @Override
            public boolean isRootNodeShown() {
                return false;
            }
        };
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return BnfLanguage.INSTANCE;
    }

    public static class MyModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider {

        protected MyModel(@Nonnull PsiFile psiFile) {
            super(psiFile, new MyElement(psiFile));
            withSuitableClasses(BnfFile.class, BnfRule.class, BnfAttrs.class, BnfAttr.class);
        }


        @Override
        public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
            return element.getValue() instanceof BnfAttrs;
        }

        @Override
        public boolean isAlwaysLeaf(StructureViewTreeElement element) {
            final Object value = element.getValue();
            return value instanceof BnfRule || value instanceof BnfAttr;
        }

        @Override
        public boolean shouldEnterElement(Object element) {
            return false;
        }

        @Override
        protected boolean isSuitable(PsiElement element) {
            return element instanceof BnfAttrs || element instanceof BnfRule;
        }
    }

    public static class MyElement extends PsiTreeElementBase<PsiElement> implements SortableTreeElement {

        public MyElement(PsiElement element) {
            super(element);
        }

        @Override
        @RequiredReadAction
        public String getAlphaSortKey() {
            return getPresentableText();
        }

        @Nonnull
        @Override
        public Collection<StructureViewTreeElement> getChildrenBase() {
            PsiElement element = getElement();
            if (element instanceof BnfRule || element instanceof BnfAttr) {
                return Collections.emptyList();
            }
            final ArrayList<StructureViewTreeElement> result = new ArrayList<>();
            if (element instanceof BnfFile bnfFile) {
                for (BnfAttrs o : bnfFile.getAttributes()) {
                    result.add(new MyElement(o));
                }
                for (BnfRule o : bnfFile.getRules()) {
                    result.add(new MyElement(o));
                }
            }
            else if (element instanceof BnfAttrs bnfAttrs) {
                for (BnfAttr o : bnfAttrs.getAttrList()) {
                    result.add(new MyElement(o));
                }
            }
            return result;
        }

        @Override
        @RequiredReadAction
        public String getPresentableText() {
            PsiElement element = getElement();
            if (element instanceof BnfRule rule) {
                return rule.getName();
            }
            else if (element instanceof BnfAttr attr) {
                return getAttrDisplayName(attr);
            }
            else if (element instanceof BnfAttrs attrs) {
                List<BnfAttr> attrList = attrs.getAttrList();
                final BnfAttr firstAttr = ContainerUtil.getFirstItem(attrList);
                if (firstAttr == null) {
                    return "Attributes { <empty> }";
                }
                String suffix = attrList.size() > 1 ? " & " + attrList.size() + " more..." : " ";
                return "Attributes { " + getAttrDisplayName(firstAttr) + suffix + "}";
            }
            else if (element instanceof BnfFileImpl file) {
                return file.getName();
            }
            return "" + element;
        }

        @RequiredReadAction
        private static String getAttrDisplayName(BnfAttr attr) {
            final BnfAttrPattern attrPattern = attr.getAttrPattern();
            final BnfExpression attrValue = attr.getExpression();
            String attrValueText = attrValue == null ? "" : attrValue instanceof BnfValueList ? "[ ... ]" : attrValue.getText();
            return attr.getName() + (attrPattern == null ? "" : attrPattern.getText()) + " = " + attrValueText;
        }

        @Override
        @RequiredReadAction
        public Image getIcon() {
            PsiElement element = getElement();
            if (element == null) {
                return null;
            }
            return element instanceof BnfAttrs ? PlatformIconGroup.nodesPackage() : IconDescriptorUpdaters.getIcon(element, 0);
        }
    }
}
