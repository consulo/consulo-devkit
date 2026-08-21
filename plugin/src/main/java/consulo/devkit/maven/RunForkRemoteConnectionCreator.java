package consulo.devkit.maven;

import com.intellij.java.debugger.engine.DebuggerUtils;
import com.intellij.java.execution.configurations.RemoteConnection;
import consulo.annotation.component.ExtensionImpl;
import consulo.java.execution.configurations.OwnJavaParameters;
import consulo.process.ExecutionException;
import jakarta.annotation.Nullable;
import org.jetbrains.idea.maven.execution.MavenRemoteConnectionCreator;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;

import java.util.List;
import java.util.regex.Pattern;

@ExtensionImpl
public class RunForkRemoteConnectionCreator implements MavenRemoteConnectionCreator {
    private static final Pattern RUN_FORK_PATTERN =
        Pattern.compile("consulo:run-(desktop-.+|web)-fork");

    @Override
    @Nullable
    public RemoteConnection createRemoteConnection(OwnJavaParameters javaParameters, MavenRunConfiguration runConfiguration) {
        List<String> programParams = javaParameters.getProgramParametersList().getList();
        boolean hasForkGoal = false;
        for (String param : programParams) {
            if (RUN_FORK_PATTERN.matcher(param).matches()) {
                hasForkGoal = true;
                break;
            }
        }

        if (!hasForkGoal) {
            return null;
        }

        String port;
        try {
            port = DebuggerUtils.getInstance().findAvailableDebugAddress(true);
        }
        catch (ExecutionException e) {
            return null;
        }

        javaParameters.getProgramParametersList().add("-Dconsulo.run.fork.debug.port=" + port);

        return new RemoteConnection(true, "127.0.0.1", port, false);
    }
}
