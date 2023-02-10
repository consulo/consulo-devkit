package consulo.devkit.grammarKit.generator;

import consulo.project.Project;

import javax.annotation.Nonnull;

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
