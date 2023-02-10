import consulo.util.xml.serializer.annotation.Attribute;

public class MyExtBean {
  @java.lang.Deprecated
  @Attribute("old")
  public String myOld;

  @Attribute("attr")
  public String myAttr;
}