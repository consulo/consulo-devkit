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

package org.intellij.grammar.config;

import java.util.function.Supplier;

/**
 * @author gregsh
 */
public interface Options {
    Supplier<String> GEN_DIR = Option.strOption("grammar.kit.gen.dir", "gen");
    Supplier<String> GEN_JFLEX_ARGS = Option.strOption("grammar.kit.gen.jflex.args", "");

    Supplier<Integer> GPUB_MAX_LEVEL = Option.intOption("grammar.kit.gpub.max.level", 1000);
}
