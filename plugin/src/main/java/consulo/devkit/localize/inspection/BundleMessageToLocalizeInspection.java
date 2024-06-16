package consulo.devkit.localize.inspection;

import com.intellij.java.language.impl.psi.impl.JavaConstantExpressionEvaluator;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.java.language.psi.util.InheritanceUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.CommonBundle;
import consulo.application.WriteAction;
import consulo.component.util.localize.AbstractBundle;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

/**
 * @author <a href="mailto:nikolay@yurchenko.su">Nikolay Yurchenko</a>
 * @since 2024-06-07
 */
@ExtensionImpl
public class BundleMessageToLocalizeInspection extends InternalInspection {
  protected static final String
    BUNDLE_SUFFIX = "Bundle",
    LOCALIZE_SUFFIX = "Localize",
    MESSAGE_METHOD_NAME = "message";

  @Nonnull
  @Override
  public String getDisplayName() {
    return DevKitLocalize.inspectionsReplaceWithXxxlocalizeTitle().get();
  }

  @Nonnull
  @Override
  public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
    return new BundleCallVisitor(holder);
  }

  public static String getString(PsiExpression expr) {
    return (String)getObject(expr);
  }

  private static Object getObject(PsiExpression expr) {
    return JavaConstantExpressionEvaluator.computeConstantExpression(expr, true);
  }

  private static class BundleCallVisitor extends JavaElementVisitor {
    private final ProblemsHolder holder;

    private BundleCallVisitor(ProblemsHolder holder) {
      this.holder = holder;
    }

    @Override
    @RequiredReadAction
    public void visitMethodCallExpression(@Nonnull PsiMethodCallExpression expression) {
      new TransformToLocalizeInspector(expression).registerProblem();
    }

    private class TransformToLocalizeInspector extends LocalizeClassExistsChecker {
      protected String myReplacementCodeBlock;
      protected LocalizeValue myInspectionName;

      protected TransformToLocalizeInspector(@Nonnull PsiMethodCallExpression expression) {
        super(expression);
      }

      @RequiredReadAction
      public void registerProblem() {
        if (!isApplicable()) {
          return;
        }

        initReplacementCodeBlock();

        myInspectionName = DevKitLocalize.inspectionsReplaceWithXxxlocalize(myLocalizeClassName, myLocalizeMethodName);

        holder.registerProblem(
          myExpression,
          myInspectionName.get(),
          new TransformToLocalizeFix(myExpression, myInspectionName, myReplacementCodeBlock)
        );
      }

      @RequiredReadAction
      private void initReplacementCodeBlock() {
        StringBuilder codeBlock = new StringBuilder()
          .append(myLocalizeClassQualifiedName)
          .append('.').append(myLocalizeMethodName).append('(');

        for (int i = 1, n = myArgExpressions.length; i < n; i++) {
          PsiExpression argExpression = myArgExpressions[i];
          if (i > 1) codeBlock.append(", ");
          codeBlock.append(argExpression.getText());
        }

        codeBlock.append(").get()");

        myReplacementCodeBlock = codeBlock.toString();
      }
    }
  }

  private static class TransformToLocalizeFix extends LocalQuickFixOnPsiElement {
    protected final LocalizeValue myInspectionName;
    protected final String myReplacementCodeBlock;

    protected TransformToLocalizeFix(
      @Nonnull PsiElement element,
      LocalizeValue inspectionName,
      String replacementCodeBlock
    ) {
      super(element);
      myInspectionName = inspectionName;
      myReplacementCodeBlock = replacementCodeBlock;
    }

    @Nonnull
    @Override
    public String getText() {
      return myInspectionName.get();
    }

    @Override
    public void invoke(
      @Nonnull Project project,
      @Nonnull PsiFile psiFile,
      @Nonnull PsiElement expression,
      @Nonnull PsiElement endElement
    ) {
      PsiExpression newExpression = JavaPsiFacade.getElementFactory(project)
        .createExpressionFromText(myReplacementCodeBlock, expression);

      WriteAction.run(() -> {
        PsiElement newElement = expression.replace(newExpression);

        JavaCodeStyleManager.getInstance(project).shortenClassReferences(newElement);
      });
    }

    @Nls
    @Nonnull
    @Override
    public String getFamilyName() {
      return "DevKit";
    }
  }

  private abstract static class MethodCallExpressionChecker {
    @Nonnull
    protected final PsiMethodCallExpression myExpression;
    @Nonnull
    protected final PsiReferenceExpression myMethodExpression;
    @Nonnull
    protected Project myProject;

    protected MethodCallExpressionChecker(@Nonnull PsiMethodCallExpression expression) {
      myExpression = expression;
      myMethodExpression = expression.getMethodExpression();
      myProject = expression.getProject();
    }

    abstract boolean isApplicable();
  }

  private static class CallsBundleMessageChecker extends MethodCallExpressionChecker {
    protected CallsBundleMessageChecker(@Nonnull PsiMethodCallExpression expression) {
      super(expression);
    }

    @Override
    @RequiredReadAction
    boolean isApplicable() {
      if (!MESSAGE_METHOD_NAME.equals(myMethodExpression.getReferenceName())) {
        return false;
      }

      PsiElement qualifier = myMethodExpression.getQualifier();
      if (qualifier == null || !qualifier.getText().endsWith(BUNDLE_SUFFIX)) {
        return false;
      }

      return !myExpression.getArgumentList().isEmpty();
    }
  }

  private static class ClassExtendsAbstractBundleChecker extends CallsBundleMessageChecker {
    @SuppressWarnings("deprecation")
    private static final String ABSTRACT_BUNDLE_CLASS_NAME = AbstractBundle.class.getName();
    private static final String COMMON_BUNDLE_CLASS_NAME = CommonBundle.class.getName();

    protected PsiElement myMethod;
    protected PsiClass myClass;

    protected ClassExtendsAbstractBundleChecker(@Nonnull PsiMethodCallExpression expression) {
      super(expression);
    }

    @Override
    @RequiredReadAction
    boolean isApplicable() {
      if (!super.isApplicable()) {
        return false;
      }

      myMethod = myMethodExpression.resolve();
      PsiElement parent = (myMethod == null) ? null : myMethod.getParent();
      myClass = (parent instanceof PsiClass psiClass) ? psiClass : null;

      return myClass != null && (InheritanceUtil.isInheritor(myClass, ABSTRACT_BUNDLE_CLASS_NAME)
        || COMMON_BUNDLE_CLASS_NAME.equals(myClass.getQualifiedName()));
    }
  }

  private static class LocalizeClassExistsChecker extends ClassExtendsAbstractBundleChecker {
    protected static final String
      ZERO_PREFIX = "zero",
      ONE_PREFIX = "one",
      TWO_PREFIX = "two";

    protected String
      myClassName,
      myLocalizeClassName,
      myLocalizeClassQualifiedName,
      myLocalizeMethodName;

    protected PsiExpression[] myArgExpressions;

    protected LocalizeClassExistsChecker(@Nonnull PsiMethodCallExpression expression) {
      super(expression);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    @RequiredReadAction
    boolean isApplicable() {
      if (!super.isApplicable()) {
        return false;
      }

      myClassName = myClass.getName();

      if (!initLocalizeClass()) {
        return false;
      }

      myArgExpressions = myExpression.getArgumentList().getExpressions();

      String key = getString(myArgExpressions[0]);
      if (key == null) {
        return false;
      }

      myLocalizeMethodName = normalizeName(capitalizeByDot(key));

      return true;
    }

    @RequiredReadAction
    private boolean initLocalizeClass() {
      PsiClass localizeClass = LocalizeClassResolver.resolveByBundle(myClass);

      if (localizeClass == null) {
        return false;
      }

      myLocalizeClassName = localizeClass.getName();
      myLocalizeClassQualifiedName = localizeClass.getQualifiedName();
      return true;
    }

    private String normalizeName(String text) {
      char c = text.charAt(0);
      if (c == '0') {
        return ZERO_PREFIX + text.substring(1, text.length());
      }
      else if (c == '1') {
        return ONE_PREFIX + text.substring(1, text.length());
      }
      else if (c == '2') {
        return TWO_PREFIX + text.substring(1, text.length());
      }
      return escapeString(text);
    }

    private String escapeString(String name) {
      return PsiNameHelper.getInstance(myProject).isIdentifier(name) ? name : "_" + name;
    }

    private String capitalizeByDot(String key) {
      String[] split = key.replace(" ", ".").split("\\.");

      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < split.length; i++) {
        if (i != 0) {
          builder.append(StringUtil.capitalize(split[i]));
        }
        else {
          builder.append(split[i]);
        }
      }

      return builder.toString();
    }
  }
}
