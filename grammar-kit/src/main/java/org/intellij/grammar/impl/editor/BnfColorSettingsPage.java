/*
 * Copyright 2011-present Greg Shrago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.grammar.impl.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.SyntaxHighlighter;

import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static org.intellij.grammar.impl.editor.BnfSyntaxHighlighter.*;

/**
 * @author gregsh
 */
@ExtensionImpl
public class BnfColorSettingsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] ATTRS;

    static {
        ATTRS = new AttributesDescriptor[]{
            new AttributesDescriptor("Illegal character", ILLEGAL),
            new AttributesDescriptor("Comment", COMMENT),
            new AttributesDescriptor("String", STRING),
            new AttributesDescriptor("Number", NUMBER),
            new AttributesDescriptor("Keyword", KEYWORD),
            new AttributesDescriptor("Token", TOKEN),
            new AttributesDescriptor("Rule", RULE),
            new AttributesDescriptor("Attribute", ATTRIBUTE),
            new AttributesDescriptor("Meta rule", META_RULE),
            new AttributesDescriptor("Meta rule parameter", META_PARAM),
            new AttributesDescriptor("Pattern", PATTERN),
            new AttributesDescriptor("External", EXTERNAL),
            new AttributesDescriptor("Parenthesis", PARENTHS),
            new AttributesDescriptor("Braces", BRACES),
            new AttributesDescriptor("Brackets", BRACKETS),
            new AttributesDescriptor("Angles", ANGLES),
            new AttributesDescriptor("Operation sign", OP_SIGN),
            new AttributesDescriptor("Pin marker", PIN_MARKER),
            new AttributesDescriptor("Recover marker", RECOVER_MARKER),
        };
    }

    @Nonnull
    public String getDisplayName() {
        return "Grammar";
    }

    @Nonnull
    public AttributesDescriptor[] getAttributeDescriptors() {
        return ATTRS;
    }

    @Nonnull
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Nonnull
    public SyntaxHighlighter getHighlighter() {
        return new BnfSyntaxHighlighter();
    }

    @Nonnull
    public String getDemoText() {
        return "/*\n" +
            " * Sample grammar\n" +
            " */\n" +
            "{\n" +
            "  <a>generatePsi</a>=<k>false</k>\n" +
            "  <a>classHeader</a>=<pa>\"header.txt\"</pa>\n" +
            "  <a>parserClass</a>=<pa>\"org.MyParser\"</pa>\n" +
            "  <a>pin</a>(<pa>\".*_list(?:_\\d.*)?\"</pa>)=1\n" +
            "  <a>tokens</a>=[\n" +
            "    <a>COMMA</a>=<pa>\",\"</pa>\n" +
            "    <a>LEFT_PAREN</a>=<pa>\"(\"</pa>\n" +
            "    <a>RIGHT_PAREN</a>=<pa>\")\"</pa>\n" +
            "  ]\n" +
            "}\n" +
            "// Grammar rules\n" +
            "<r>root</r> ::= <r>header</r> <r>content</r>\n" +
            "<r>header</r> ::= <t>DECLARE</t> <r>reference</r>\n" +
            "<k>external</k> <r>reference</r> ::= <e>parseReference</e>\n" +
            "<k>private</k> <k>meta</k> <mr>comma_list</mr> ::= <pin><s>'('</s></pin> <mp><<p>></mp> (<pin><s>','</s></pin> <mp><<p>></mp>) * ')'\n" +
            "<k>private</k> <r>content</r> ::= <pin><t>AS</t></pin> <<<mr>comma_list</mr> <ru><r>element</r></ru>>> {<a>pin</a>=1}\n" +
            "<ru><r>element</r></ru> ::= <r>reference</r> [ {<pa>'+'</pa> | <pa>'-'</pa>} <r>reference</r> <t>ONLY</t>?] {<a>recoverWhile</a>=<r>element_recover</r>}\n" +
            "<k>private</k> <r>element_recover</r> ::= !(',' | ')')\n" +
            "\n";
    }

    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        final Map<String, TextAttributesKey> map = new HashMap<>();
        map.put("r", RULE);
        map.put("mr", META_RULE);
        map.put("a", ATTRIBUTE);
        map.put("pa", PATTERN);
        map.put("t", TOKEN);
        map.put("k", KEYWORD);
        map.put("e", EXTERNAL);
        map.put("pin", PIN_MARKER);
        map.put("s", STRING);
        map.put("ru", RECOVER_MARKER);
        map.put("mp", META_PARAM);
        return map;
    }
}