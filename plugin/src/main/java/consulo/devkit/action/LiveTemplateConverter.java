package consulo.devkit.action;

import com.intellij.java.indexing.search.searches.ClassInheritorsSearch;
import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.psi.*;
import com.palantir.javapoet.*;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.language.editor.template.LiveTemplateContributor;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.jdom.Element;

import javax.lang.model.element.Modifier;
import java.util.*;

/**
 * @author VISTALL
 * @since 2024-10-25
 */
public class LiveTemplateConverter {
    private static class Template {
        String myId;
        String myAbbreviation;
        String myValue;

        boolean myToReformat;

        CodeBlock myDescriptionBlock;

        List<Variable> myVariables = new ArrayList<>();

        Map<String, Boolean> myContexts = new LinkedHashMap<>();

        Boolean myToShortenFQNames;
    }

    private record Variable(String name, String expression, String defaultValue, boolean alwaysStopAt) {

    }

    private final Project myProject;
    private final Module myModule;
    private final GlobalSearchScope mySearchScope;

    public LiveTemplateConverter(Project project, Module module) {
        myProject = project;
        myModule = module;
        mySearchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
    }

    public TypeSpec read(String fileName, Element rootElement) {
        String group = rootElement.getAttributeValue("group", fileName);

        String groupId = group.replace(" ", "");
        groupId = groupId.replace("#", "sharp");
        groupId = groupId.toLowerCase(Locale.US);

        List<Template> templates = new ArrayList<>();

        Map<String, String> contextTypes = ReadAction.compute(() -> {
            PsiClass contextClass = JavaPsiFacade.getInstance(myProject).findClass(TemplateContextType.class.getName(), mySearchScope);

            if (contextClass == null) {
                return Map.of();
            }

            Map<String, String> types = new HashMap<>();
            PsiClass[] classes = ClassInheritorsSearch.search(contextClass, mySearchScope, true).toArray(PsiClass.EMPTY_ARRAY);
            for (PsiClass aClass : classes) {
                if (aClass.getLanguage() == JavaLanguage.INSTANCE && aClass instanceof PsiCompiledElement) {
                    continue;
                }

                for (PsiMethod constructor : aClass.getConstructors()) {
                    PsiCodeBlock body = constructor.getBody();
                    if (body == null) {
                        continue;
                    }

                    for (PsiStatement statement : body.getStatements()) {
                        if (!(statement instanceof PsiExpressionStatement expressionStatement)) {
                            continue;
                        }

                        PsiExpression expression = expressionStatement.getExpression();

                        if (!(expression instanceof PsiMethodCallExpression methodCallExpression)) {
                            continue;
                        }

                        PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) expression).getMethodExpression();
                        String text = methodExpression.getText();
                        if (PsiKeyword.SUPER.equals(text) || PsiKeyword.THIS.equals(text)) {
                            PsiExpression[] expressions = methodCallExpression.getArgumentList().getExpressions();

                            if (expressions.length == 0) {
                                continue;
                            }

                            PsiExpression firstExpression = expressions[0];
                            if (firstExpression instanceof PsiLiteralExpression literalExpression) {
                                Object value = literalExpression.getValue();
                                if (value instanceof String str) {
                                    types.put(str, aClass.getQualifiedName());
                                }
                            }
                        }
                    }
                }
            }
            return types;
        });
        for (Element templateElement : rootElement.getChildren("template")) {
            String name = templateElement.getAttributeValue("name");
            String value = templateElement.getAttributeValue("value");

            String description = templateElement.getAttributeValue("description");
            String resourceBundle = templateElement.getAttributeValue("resource-bundle");
            String resourceBundleKey = templateElement.getAttributeValue("key");

            boolean toReformat = Boolean.parseBoolean(templateElement.getAttributeValue("toReformat", "false"));
            String toShortenFQNamesStr = templateElement.getAttributeValue("toShortenFQNames");

            Template template = new Template();
            template.myId = groupId + StringUtil.capitalize(name);
            template.myAbbreviation = name;
            template.myValue = value;
            if (toShortenFQNamesStr != null) {
                template.myToShortenFQNames = Boolean.parseBoolean(toShortenFQNamesStr);
            }

            if (!StringUtil.isEmptyOrSpaces(description)) {
                template.myDescriptionBlock = CodeBlock.of("$T.localizeTODO($S)", TypeName.get(LocalizeValue.class), description);
            } else if (resourceBundle != null && resourceBundleKey != null){
                String bundleName = StringUtil.getShortName(resourceBundle);
                bundleName = bundleName.replace("Bundle", "Localize");

                List<String> messages = StringUtil.split(resourceBundleKey, ".");
                StringBuilder body = new StringBuilder();
                body.append(bundleName);
                body.append(".");
                for (int i = 0; i < messages.size(); i++) {
                    String part = messages.get(i);

                    if (i != 0) {
                        body.append(StringUtil.capitalize(part));
                    } else {
                        body.append(part);
                    }
                }
                body.append("()");

                template.myDescriptionBlock = CodeBlock.of(body.toString());
            }  else {
                template.myDescriptionBlock = CodeBlock.of("$T.localizeTODO($S)", TypeName.get(LocalizeValue.class), name);
            }

            template.myToReformat = toReformat;

            templates.add(template);

            for (Element variableElement : templateElement.getChildren("variable")) {
                String varName = variableElement.getAttributeValue("name");
                String varExpr = variableElement.getAttributeValue("expression");
                String varDefaultValue = variableElement.getAttributeValue("defaultValue", "");
                boolean varAlwaysStopAt = Boolean.parseBoolean(variableElement.getAttributeValue("alwaysStopAt", "false"));

                template.myVariables.add(new Variable(varName, varExpr, varDefaultValue, varAlwaysStopAt));
            }

            Element contextElement = templateElement.getChild("context");
            if (contextElement != null) {
                for (Element optionElement : contextElement.getChildren("option")) {
                    String optionName = optionElement.getAttributeValue("name");
                    boolean optionValue = Boolean.parseBoolean(optionElement.getAttributeValue("value", "true"));

                    String className = contextTypes.getOrDefault(optionName, optionName);
                    template.myContexts.put(className, optionValue);
                }
            }
        }

        String typeSpecName = group.replace("#", "Sharp").replace(" ", "") + "LiveTemplateContributor";

        TypeSpec.Builder builder = TypeSpec.classBuilder(typeSpecName);
        builder.addAnnotation(ExtensionImpl.class);
        builder.addSuperinterface(LiveTemplateContributor.class);
        builder.addModifiers(Modifier.PUBLIC);

        builder.addMethod(MethodSpec.methodBuilder("groupId")
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addAnnotation(Override.class)
            .addAnnotation(Nonnull.class)
            .addCode(CodeBlock.of("return $S;", groupId))
            .build());
        
        builder.addMethod(MethodSpec.methodBuilder("groupName")
            .addModifiers(Modifier.PUBLIC)
            .returns(LocalizeValue.class)
            .addAnnotation(Override.class)
            .addAnnotation(Nonnull.class)
            .addCode(CodeBlock.of("return $T.localizeTODO($S);", TypeName.get(LocalizeValue.class), group))
            .build());

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("contribute");
        methodBuilder.addModifiers(Modifier.PUBLIC);
        methodBuilder.addAnnotation(Override.class);
        methodBuilder.addParameter(ParameterSpec.builder(LiveTemplateContributor.Factory.class, "factory").addAnnotation(Nonnull.class).build());
        
        for (Template template : templates) {
            CodeBlock.Builder codeBuilder = CodeBlock.builder();
            codeBuilder.beginControlFlow("try(Builder builder = factory.newBuilder($S, $S, $S, $L))", template.myId, template.myAbbreviation, template.myValue, template.myDescriptionBlock);

            if (template.myToReformat) {
                codeBuilder.add("builder.withReformat();\n\n");
            }

            if (template.myToShortenFQNames != null) {
                codeBuilder.add("builder.withOption(ShortenFQNamesProcessor.KEY, $L);\n\n", template.myToShortenFQNames);
            }

            for (Variable variable : template.myVariables) {
                codeBuilder.add("builder.withVariable($S, $S, $S, $L);\n", variable.name(), variable.expression(), variable.defaultValue(), variable.alwaysStopAt());
            }

            codeBuilder.add("\n");

            for (Map.Entry<String, Boolean> entry : template.myContexts.entrySet()) {
                String className = entry.getKey();
                Boolean value = entry.getValue();

                codeBuilder.add("builder.withContext($T.class, $L);\n", ClassName.bestGuess(className), value);
            }

            codeBuilder.endControlFlow();

            methodBuilder.addCode(codeBuilder.build());

            methodBuilder.addCode("\n");
        }
        builder.addMethod(methodBuilder.build());

        return builder.build();
    }
}
