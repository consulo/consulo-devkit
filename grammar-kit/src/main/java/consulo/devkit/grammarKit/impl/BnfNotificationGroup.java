package consulo.devkit.grammarKit.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.devkit.grammarKit.generator.ErrorReporter;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;
import consulo.project.ui.notification.NotificationType;
import jakarta.annotation.Nonnull;
import org.intellij.grammar.impl.actions.GenerateAction;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 03/02/2023
 */
@ExtensionImpl
public class BnfNotificationGroup implements NotificationGroupContributor {
    public static final NotificationGroup GRAMMAR_KIT =
        NotificationGroup.balloonGroup("BnfGrammarKit", LocalizeValue.localizeTODO("Grammar Kit"));

    @Override
    public void contribute(@Nonnull Consumer<NotificationGroup> consumer) {
        consumer.accept(GRAMMAR_KIT);
        consumer.accept(GenerateAction.LOG_GROUP);

        ErrorReporter.ourInstance = new ErrorReporter() {
            @Override
            public void reportWarning(@Nonnull Project project, @Nonnull String text) {
                GenerateAction.LOG_GROUP.createNotification(text, NotificationType.WARNING).notify(project);
            }
        };
    }
}
