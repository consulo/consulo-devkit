/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.actions;

import com.intellij.java.language.psi.PsiClass;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.xml.psi.xml.XmlFile;
import jakarta.annotation.Nonnull;
import org.jetbrains.idea.devkit.util.ActionType;

import java.util.function.Consumer;

/**
 * @author yole
 */
public class NewActionAction extends GeneratePluginClassAction {
    private NewActionDialog myDialog;

    public NewActionAction() {
        super(DevKitLocalize.newMenuActionText(), DevKitLocalize.newMenuActionDescription(), null);
    }

    @Override
    @RequiredUIAccess
    protected void invokeDialogImpl(Project project, PsiDirectory directory, @Nonnull Consumer<PsiElement[]> consumer) {
        final MyInputValidator validator = new MyInputValidator(project, directory);

        myDialog = new NewActionDialog(project);
        myDialog.showAsync().doWhenDone(() -> consumer.accept(validator.getCreatedElements()));
    }

    protected String getClassTemplateName() {
        return "Action.java";
    }

    public void patchPluginXml(final XmlFile pluginXml, final PsiClass klass) throws IncorrectOperationException {
        ActionType.ACTION.patchPluginXml(pluginXml, klass, myDialog);
    }

    protected String getErrorTitle() {
        return DevKitLocalize.newActionError().get();
    }

    protected String getCommandName() {
        return DevKitLocalize.newActionCommand().get();
    }

    protected String getActionName(PsiDirectory directory, String newName) {
        return DevKitLocalize.newActionActionName(directory, newName).get();
    }
}
