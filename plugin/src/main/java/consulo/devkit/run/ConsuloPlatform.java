package consulo.devkit.run;

/**
 * @author VISTALL
 * @since 27/04/2021
 */
public enum ConsuloPlatform
{
	WEB("consulo.web.bootstrap", "consulo.web.boot.main.Main"),
	DESKTOP_SWT("consulo.desktop.swt.bootstrap", "consulo.desktop.swt.boot.main.Main"),
	DESKTOP_AWT("consulo.desktop.awt.bootstrap", "consulo.desktop.awt.boot.main.Main"),
	// old desktop Main
	DESKTOP_AWT_V2("consulo.desktop.bootstrap", "consulo.desktop.boot.main.Main");

	private final String myModuleName;
	private final String myMainClass;

	ConsuloPlatform(String moduleName, String mainClass)
	{
		myModuleName = moduleName;
		myMainClass = mainClass;
	}

	public String getModuleName()
	{
		return myModuleName;
	}

	public String getMainClass()
	{
		return myMainClass;
	}
}

