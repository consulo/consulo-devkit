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

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.impl.psi.impl.JavaConstantExpressionEvaluator;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.PsiTypesUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.psi.ElementColorProvider;
import consulo.language.psi.PsiElement;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Initial version from {@link com.intellij.java.impl.codeInsight.daemon.impl.JavaColorProvider}
 *
 * @author VISTALL
 * @since 2017-10-12
 */
@ExtensionImpl
public class UIColorLineMarkerProvider implements ElementColorProvider {
    @Nonnull
    @Override
    public Language getLanguage() {
        return JavaLanguage.INSTANCE;
    }

    @Override
    @RequiredReadAction
    public ColorValue getColorFrom(@Nonnull PsiElement element) {
        return getColorFromExpression(element);
    }

    public static boolean isColorType(@Nullable PsiType type) {
        if (type != null) {
            PsiClass aClass = PsiTypesUtil.getPsiClass(type);
            if (aClass != null) {
                String fqn = aClass.getQualifiedName();
                if (RGBColor.class.getName().equals(fqn)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static ColorValue getColorFromExpression(@Nullable PsiElement element) {
        return element instanceof PsiNewExpression expr && isColorType(expr.getType()) ? getColor(expr.getArgumentList()) : null;
    }

    @Nullable
    private static ColorValue getColor(PsiExpressionList list) {
        try {
            PsiExpression[] args = list.getExpressions();
            PsiType[] types = list.getExpressionTypes();
            ColorConstructors type = getConstructorType(types);
            if (type != null) {
                return switch (type) {
                    case INTx3 -> new RGBColor(getInt(args[0]), getInt(args[1]), getInt(args[2]));
                    case INTx3_FLOAT -> {
                        float alpha = getFloat(args[3]);
                        yield new RGBColor(getInt(args[0]), getInt(args[1]), getInt(args[2]), alpha);
                    }
                };
            }
        }
        catch (Exception ignore) {
        }
        return null;
    }

    @Nullable
    private static ColorConstructors getConstructorType(PsiType[] types) {
        return switch (types.length) {
            case 3 -> ColorConstructors.INTx3;
            case 4 -> ColorConstructors.INTx3_FLOAT;
            default -> null;
        };
    }

    public static int getInt(PsiExpression expr) {
        return (Integer) getObject(expr);
    }

    public static float getFloat(PsiExpression expr) {
        return (Float) getObject(expr);
    }

    private static Object getObject(PsiExpression expr) {
        return JavaConstantExpressionEvaluator.computeConstantExpression(expr, true);
    }

    @Override
    @RequiredWriteAction
    public void setColorTo(@Nonnull PsiElement element, @Nonnull ColorValue color) {
        PsiExpressionList argumentList = ((PsiNewExpression) element).getArgumentList();
        assert argumentList != null;

        PsiExpression[] expr = argumentList.getExpressions();
        ColorConstructors type = getConstructorType(argumentList.getExpressionTypes());

        RGBColor rgb = color.toRGB();
        replaceInt(expr[0], rgb.getRed());
        replaceInt(expr[1], rgb.getGreen());
        replaceInt(expr[2], rgb.getBlue());

        if (type == ColorConstructors.INTx3_FLOAT) {
            replaceFloat(expr[3], rgb.getAlpha());
        }
    }

    @RequiredWriteAction
    private static void replaceInt(PsiExpression expr, int newValue) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(expr.getProject());
        if (getInt(expr) != newValue) {
            String text = Integer.toString(newValue);
            expr.replace(factory.createExpressionFromText(text, null));
        }
    }

    @RequiredWriteAction
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
