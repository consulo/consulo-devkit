package consulo.devkit.grammarKit.generator;

import javax.annotation.Nonnull;

import com.intellij.openapi.project.Project;

/**
 * @author VISTALL
 * @since 2018-06-17
 */
public abstract class ErrorReporter
{
	public static ErrorReporter ourInstance = new ErrorReporter()
	{
		@Override
		public void reportWarning(@Nonnull Project project, @Nonnull String text)
		{
			System.out.println(text);
		}
	};

	public abstract void reportWarning(@Nonnull Project project, @Nonnull String text);
}
