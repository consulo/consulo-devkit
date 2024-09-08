package consulo.devkit.action;

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.lang.properties.psi.PropertiesFile;
import consulo.language.editor.CommonDataKeys;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public class ConvertResourceBundleToYamlAction extends InternalAction {
    public ConvertResourceBundleToYamlAction() {
        super("Convert resource bundle to YAML");
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent event) {
        PsiFile file = event.getData(CommonDataKeys.PSI_FILE);

        PropertiesFile propertiesFile = (PropertiesFile)file;

        assert propertiesFile != null;

        HashMap<String, Map<String, String>> messages = new LinkedHashMap<>();

        for (IProperty property : propertiesFile.getProperties()) {
            Map<String, String> messageInfo = new LinkedHashMap<>();
            messageInfo.put("text", property.getUnescapedValue());

            messages.put(property.getUnescapedKey(), messageInfo);
        }

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setAllowUnicode(true);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setWidth(1024);
        dumperOptions.setMaxSimpleKeyLength(1024);
        Yaml yaml = new Yaml(dumperOptions);

        File ioFile = VirtualFileUtil.virtualToIoFile(file.getVirtualFile());

        File parentFile = ioFile.getParentFile();

        File result = new File(parentFile, file.getVirtualFile().getNameWithoutExtension() + ".yaml");
        try (Writer fileWriter = new OutputStreamWriter(new FileOutputStream(result), StandardCharsets.UTF_8)) {
            yaml.dump(messages, fileWriter);
        }
        catch (IOException ignored) {
        }

        LocalFileSystem.getInstance().refreshIoFiles(Arrays.asList(result));
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent event) {
        super.update(event);

        if (!event.getPresentation().isEnabled()) {
            return;
        }

        PsiFile file = event.getData(CommonDataKeys.PSI_FILE);
        if (file == null || file.getFileType() != PropertiesFileType.INSTANCE) {
            event.getPresentation().setEnabledAndVisible(false);
        }
    }
}
