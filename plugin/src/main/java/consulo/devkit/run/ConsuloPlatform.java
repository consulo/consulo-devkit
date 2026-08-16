package consulo.devkit.run;

/**
 * @author VISTALL
 * @since 2021-04-27
 */
public enum ConsuloPlatform {
    WEB("consulo.web.bootstrap", "consulo.web.boot.main.Main"),
    DESKTOP_SWT("consulo.desktop.swt.bootstrap", "consulo.desktop.swt.boot.main.Main"),
    DESKTOP_AWT("consulo.desktop.awt.bootstrap", "consulo.desktop.awt.boot.main.Main"),
    DESKTOP_QT("consulo.desktop.qt.bootstrap", "consulo.desktop.qt.boot.main.Main");

    private final String myModuleName;
    private final String myMainClass;

    ConsuloPlatform(String moduleName, String mainClass) {
        myModuleName = moduleName;
        myMainClass = mainClass;
    }

    public String getModuleName() {
        return myModuleName;
    }

    public String getMainClass() {
        return myMainClass;
    }
}

