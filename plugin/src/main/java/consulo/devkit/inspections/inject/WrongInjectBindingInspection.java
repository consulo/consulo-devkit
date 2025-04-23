package consulo.devkit.inspections.inject;

import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.PsiFormatUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.CachedValueProvider;
import consulo.devkit.DevKitComponentScope;
import consulo.devkit.inspections.valhalla.ExtensionImplUtil;
import consulo.devkit.inspections.valhalla.ValhallaClasses;
import consulo.devkit.localize.DevKitLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.idea.devkit.inspections.internal.InternalInspection;

import java.util.Set;

/**
 * @author VISTALL
 * @since 2022-08-10
 */
@ExtensionImpl
public class WrongInjectBindingInspection extends InternalInspection {
    private static final Set<String> PROVIDERS = Set.of("jakarta.inject.Provider");

    private static final String FILE_TYPE_REGISTRY = "consulo.virtualFileSystem.fileType.FileTypeRegistry";
    private static final String CONTAINER_PATH_MANAGER = "consulo.container.boot.ContainerPathManager";

    private static final Set<String> APPLICATION_IMPLICIT =
        Set.of("consulo.application.Application", FILE_TYPE_REGISTRY, CONTAINER_PATH_MANAGER);
    private static final Set<String> PROJECT_IMPLICIT =
        Set.of("consulo.application.Application", "consulo.project.Project", FILE_TYPE_REGISTRY, CONTAINER_PATH_MANAGER);
    private static final Set<String> MODULE_IMPLICIT = Set.of(
        "consulo.application.Application",
        "consulo.project.Project",
        "consulo.module.Module",
        FILE_TYPE_REGISTRY,
        CONTAINER_PATH_MANAGER
    );

    @Nonnull
    @Override
    public String getDisplayName() {
        return DevKitLocalize.wrongInjectBindingInspectionDisplayName().get();
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public PsiElementVisitor buildInternalVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitParameter(@Nonnull PsiParameter parameter) {
                if (!(parameter.getDeclarationScope() instanceof PsiMethod declarationScope)) {
                    return;
                }

                DevKitComponentScope thisScope = resolveComponentScope(declarationScope);

                if (thisScope != null) {
                    DevKitComponentScope injectScope = resolveServiceScope(parameter.getType(), thisScope, true);
                    if (injectScope == null || injectScope.ordinal() > thisScope.ordinal()) {
                        holder.newProblem(getDescription(thisScope, injectScope, parameter.getType()))
                            .range(parameter.getTypeElement())
                            .create();
                    }
                }
            }
        };
    }

    @Nonnull
    private LocalizeValue getDescription(
        @Nonnull DevKitComponentScope scope,
        @Nullable DevKitComponentScope targetScope,
        @Nonnull PsiType targetType
    ) {
        if (targetScope == null) {
            return DevKitLocalize.wrongInjectBindingInspectionMessageUnknown(StringUtil.SINGLE_QUOTER.apply(PsiFormatUtil.formatType(
                targetType,
                0,
                PsiSubstitutor.EMPTY
            )));
        }

        return DevKitLocalize.wrongInjectBindingInspectionMessageTooHigh(scope, targetScope);
    }

    @Nullable
    @RequiredReadAction
    private static DevKitComponentScope resolveServiceScope(
        @Nullable PsiType type,
        @Nonnull DevKitComponentScope targetScope,
        boolean providerCheck
    ) {
        if (!(type instanceof PsiClassType classType)) {
            return null;
        }

        PsiClassType.ClassResolveResult classResolveResult = classType.resolveGenerics();

        PsiClass element = classResolveResult.getElement();
        if (element == null) {
            return null;
        }

        // some implicit bindings, without annotation
        Set<String> implicitBindings = getImplicitBindings(targetScope);
        if (implicitBindings.contains(element.getQualifiedName())) {
            return targetScope;
        }

        // it's service api
        PsiAnnotation serviceApiAnno = AnnotationUtil.findAnnotationInHierarchy(element, Set.of(ValhallaClasses.SERVICE_API));
        if (serviceApiAnno != null) {
            PsiAnnotationMemberValue value = serviceApiAnno.findDeclaredAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
            if (value != null) {
                return ExtensionImplUtil.resolveScope(value);
            }
        }

        if (providerCheck && PROVIDERS.contains(element.getQualifiedName())) {
            PsiSubstitutor substitutor = classResolveResult.getSubstitutor();
            PsiType genericIn = ContainerUtil.getFirstItem(substitutor.getSubstitutionMap().values());
            return resolveServiceScope(genericIn, targetScope, false);
        }

        return null;
    }

    @Nonnull
    private static Set<String> getImplicitBindings(DevKitComponentScope scope) {
        return switch (scope) {
            case APPLICATION -> APPLICATION_IMPLICIT;
            case PROJECT -> PROJECT_IMPLICIT;
            case MODULE -> MODULE_IMPLICIT;
            default -> throw new UnsupportedOperationException();
        };
    }

    @Nullable
    private static DevKitComponentScope resolveComponentScope(PsiMethod method) {
        return LanguageCachedValueUtil.getCachedValue(
            method,
            () -> CachedValueProvider.Result.create(
                resolveComponentScopeImpl(method),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        );
    }

    @Nullable
    @RequiredReadAction
    private static DevKitComponentScope resolveComponentScopeImpl(PsiMethod method) {
        if (!method.isConstructor()) {
            return null;
        }

        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return null;
        }

        // owner contains inject annotation
        if (AnnotationUtil.isAnnotated(method, NoInjectAnnotationInspection.INJECT_ANNOTATIONS, 0)) {
            for (Couple<String> apiAndImpl : ValhallaClasses.API_TO_IMPL) {
                PsiAnnotation implAnno = AnnotationUtil.findAnnotation(containingClass, apiAndImpl.getSecond());
                if (implAnno != null) {
                    PsiAnnotationMemberValue value = implAnno.findDeclaredAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
                    if (value != null) {
                        return ExtensionImplUtil.resolveScope(value);
                    }

                    PsiAnnotation apiAnno = AnnotationUtil.findAnnotationInHierarchy(containingClass, Set.of(apiAndImpl.getFirst()));
                    if (apiAnno != null) {
                        value = apiAnno.findDeclaredAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
                        if (value != null) {
                            return ExtensionImplUtil.resolveScope(value);
                        }
                    }
                }
            }
        }
        return null;
    }
}
