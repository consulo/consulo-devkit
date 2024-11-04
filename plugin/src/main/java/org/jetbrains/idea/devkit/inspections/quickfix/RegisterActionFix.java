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
package org.jetbrains.idea.devkit.inspections.quickfix;

import com.intellij.java.language.psi.PsiClass;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.xml.psi.xml.XmlFile;
import org.jetbrains.idea.devkit.actions.NewActionDialog;
import org.jetbrains.idea.devkit.util.ActionType;

import jakarta.annotation.Nonnull;

public class RegisterActionFix extends AbstractRegisterFix {
    private NewActionDialog myDialog;

    public RegisterActionFix(PsiClass klass) {
        super(klass);
    }

    @Override
    protected String getType() {
        return DevKitLocalize.newMenuActionText().get();
    }

    @Override
    @RequiredUIAccess
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        try {
            myDialog = new NewActionDialog(myClass);
            myDialog.show();

            if (myDialog.isOK()) {
                super.applyFix(project, descriptor);
            }
        }
        finally {
            myDialog = null;
        }
    }

    @Override
    public void patchPluginXml(XmlFile pluginXml, PsiClass aClass) throws IncorrectOperationException {
        if (ActionType.GROUP.isOfType(aClass)) {
            ActionType.GROUP.patchPluginXml(pluginXml, aClass, myDialog);
        }
        else {
            ActionType.ACTION.patchPluginXml(pluginXml, aClass, myDialog);
        }
    }
}
