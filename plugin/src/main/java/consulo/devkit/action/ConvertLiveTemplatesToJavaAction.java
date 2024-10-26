package consulo.devkit.action;

import com.intellij.java.language.impl.JavaFileType;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.progress.Task;
import consulo.language.editor.WriteCommandAction;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.util.jdom.JDOMUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.ide.highlighter.XmlFileType;
import jakarta.annotation.Nonnull;
import org.jdom.Element;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 2024-10-25
 */
@ActionImpl(id = "ConvertLiveTemplatesToJavaAction", parents = @ActionParentRef(@ActionRef(id = "ProjectViewPopupMenu")))
public class ConvertLiveTemplatesToJavaAction extends InternalAction {
    private static final String ourliveTemplatesDir = "liveTemplates";

    public ConvertLiveTemplatesToJavaAction() {
        super("Convert LiveTemplates to Java Code");
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        VirtualFile file = e.getData(VirtualFile.KEY);
        if (file == null) {
            return;
        }

        Project project = e.getData(Project.KEY);
        assert project != null;
        Module module = e.getData(Module.KEY);
        assert module != null;

        Task.Modal.queue(project, "Generating Java Code...", false, indicator -> {
            indicator.setIndeterminate(true);

            try {
                LiveTemplateConverter converter = new LiveTemplateConverter(project, module);

                Element rootElement;
                try (InputStream stream = file.getInputStream()) {
                    rootElement = JDOMUtil.load(stream);
                }

                TypeSpec spec = converter.read(file.getNameWithoutExtension(), rootElement);

                String name = spec.name();

                VirtualFile parentDir = file.getParent();

                project.getUIAccess().give(() -> {
                    try {
                        WriteCommandAction.<VirtualFile, Exception>runWriteCommandAction(project, () -> {
                            VirtualFile data = parentDir.createChildData(null, name + JavaFileType.DOT_DEFAULT_EXTENSION);
                            data.setBinaryContent(JavaFile.builder("", spec).build().toString().getBytes(StandardCharsets.UTF_8));
                            return data;
                        });
                    }
                    catch (Exception e1) {
                        project.getUIAccess().give(() -> Alerts.okError(LocalizeValue.of(e1.getMessage())).showAsync(project));
                    }
                });
            }
            catch (Exception e1) {
                project.getUIAccess().give(() -> Alerts.okError(LocalizeValue.of(e1.getMessage())).showAsync(project));
            }
        });
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);

        Presentation presentation = e.getPresentation();
        if (!presentation.isVisible()) {
            return;
        }

        VirtualFile file = e.getData(VirtualFile.KEY);
        if (file == null || file.getFileType() != XmlFileType.INSTANCE) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        VirtualFile parent = file.getParent();
        if (parent == null || !ourliveTemplatesDir.equals(parent.getName())) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        presentation.setEnabledAndVisible(true);
    }
}
