/*
 * Copyright 2013-2017 consulo.io
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

package consulo.devkit.codeInsight.daemon;

import com.intellij.java.language.impl.psi.impl.JavaConstantExpressionEvaluator;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.PsiTypesUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.ElementColorProvider;
import consulo.language.psi.PsiElement;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Initial version from {@link com.intellij.codeInsight.daemon.impl.JavaColorProvider}
 *
 * @author VISTALL
 * @since 2017-10-12
 */
@ExtensionImpl
public class UIColorLineMarkerProvider implements ElementColorProvider {
    @RequiredReadAction
    @Override
    public ColorValue getColorFrom(@Nonnull PsiElement element) {
        return getColorFromExpression(element);
    }

    public static boolean isColorType(@Nullable PsiType type) {
        if (type != null) {
            final PsiClass aClass = PsiTypesUtil.getPsiClass(type);
            if (aClass != null) {
                final String fqn = aClass.getQualifiedName();
                if (RGBColor.class.getName().equals(fqn)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static ColorValue getColorFromExpression(@Nullable PsiElement element) {
        if (element instanceof PsiNewExpression) {
            final PsiNewExpression expr = (PsiNewExpression)element;
            if (isColorType(expr.getType())) {
                return getColor(expr.getArgumentList());
            }
        }
        return null;
    }

    @Nullable
    private static ColorValue getColor(PsiExpressionList list) {
        try {
            final PsiExpression[] args = list.getExpressions();
            final PsiType[] types = list.getExpressionTypes();
            ColorConstructors type = getConstructorType(types);
            if (type != null) {
                switch (type) {
                    case INTx3:
                        return new RGBColor(getInt(args[0]), getInt(args[1]), getInt(args[2]));
                    case INTx3_FLOAT:
                        float alpha = getFloat(args[3]);
                        return new RGBColor(getInt(args[0]), getInt(args[1]), getInt(args[2]), alpha);
                }
            }
        }
        catch (Exception ignore) {
        }
        return null;
    }

    @Nullable
    private static ColorConstructors getConstructorType(PsiType[] types) {
        int len = types.length;
        if (len == 0) {
            return null;
        }

        switch (len) {
            case 3:
                return ColorConstructors.INTx3;
            case 4:
                return ColorConstructors.INTx3_FLOAT;
        }

        return null;
    }

    public static int getInt(PsiExpression expr) {
        return (Integer)getObject(expr);
    }

    public static float getFloat(PsiExpression expr) {
        return (Float)getObject(expr);
    }

    private static Object getObject(PsiExpression expr) {
        return JavaConstantExpressionEvaluator.computeConstantExpression(expr, true);
    }

    @RequiredWriteAction
    @Override
    public void setColorTo(@Nonnull PsiElement element, @Nonnull ColorValue color) {
        PsiExpressionList argumentList = ((PsiNewExpression)element).getArgumentList();
        assert argumentList != null;

        PsiExpression[] expr = argumentList.getExpressions();
        ColorConstructors type = getConstructorType(argumentList.getExpressionTypes());

        assert type != null;

        switch (type) {
            case INTx3:
            case INTx3_FLOAT:
                RGBColor rgb = color.toRGB();
                replaceInt(expr[0], rgb.getRed());
                replaceInt(expr[1], rgb.getGreen());
                replaceInt(expr[2], rgb.getBlue());

                if (type == ColorConstructors.INTx3_FLOAT) {
                    replaceFloat(expr[3], rgb.getAlpha());
                }
        }
    }

    private static void replaceInt(PsiExpression expr, int newValue) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(expr.getProject());
        if (getInt(expr) != newValue) {
            String text = Integer.toString(newValue);
            expr.replace(factory.createExpressionFromText(text, null));
        }
    }

    private static void replaceFloat(PsiExpression expr, float newValue) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(expr.getProject());
        if (getFloat(expr) != newValue) {
            expr.replace(factory.createExpressionFromText(String.valueOf(newValue) + "f", null));
        }
    }

    private enum ColorConstructors {
        INTx3,
        INTx3_FLOAT
    }
}
