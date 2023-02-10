import consulo.util.xml.serializer.annotation.Attribute;

import java.lang.String;

public class ExtBeanWithAccessors {
  private String field;

  @Attribute("param")
  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }
}