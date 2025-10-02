package consulo.devkit.localize.java;

import com.intellij.java.language.impl.psi.impl.light.LightPsiClassBuilder;
import consulo.devkit.localize.LocalizeUtil;
import consulo.language.psi.PsiFile;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.List;

/**
 * @author VISTALL
 * @since 2025-10-02
 */
public class LocalizeClassBuilder extends LightPsiClassBuilder {
    private final String myPackageName;
    @Nonnull
    private final YAMLFile myYamlFile;

    public LocalizeClassBuilder(@Nonnull YAMLFile yamlFile,
                                @Nonnull String qualifiedName) {
        super(yamlFile, StringUtil.getShortName(qualifiedName));
        myYamlFile = yamlFile;
        setNavigationElement(yamlFile);

        myPackageName = qualifiedName;

        List<YAMLDocument> documents = yamlFile.getDocuments();

        for (YAMLDocument document : documents) {
            if (document.getTopLevelValue() instanceof YAMLMapping topLevelMapping) {
                for (YAMLKeyValue value : topLevelMapping.getKeyValues()) {
                    if (value.getValue() instanceof YAMLMapping valueMapping) {
                        YAMLKeyValue text = valueMapping.getKeyValueByKey("text");
                        if (text != null) {
                            String key = value.getKeyText();

                            String methodName = LocalizeUtil.formatMethodName(yamlFile.getProject(), key);

                            LocalizeMethodBuilder builder = new LocalizeMethodBuilder(this, value, methodName);

                            addMethod(builder);
                        }
                    }
                }
            }
        }
    }

    @Override
    public PsiFile getContainingFile() {
        return myYamlFile;
    }

    @Nullable
    @Override
    public String getQualifiedName() {
        return myPackageName;
    }
}
