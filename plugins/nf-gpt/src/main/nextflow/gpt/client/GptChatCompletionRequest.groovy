/*
 * Copyright 2013-2024, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nextflow.gpt.client

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.ToString

/**
 * Model a GTP chat conversation create request object.
 *
 * See also
 * https://platform.openai.com/docs/api-reference/chat/create
 * 
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
@ToString(includePackage = false, includeNames = true)
class GptChatCompletionRequest {

    @ToString(includePackage = false, includeNames = true)
    @CompileStatic
    static class Message {
        String role
        String content
    }

    @ToString(includePackage = false, includeNames = true)
    @CompileStatic
    static class ToolMessage extends Message {
        String name
        String tool_call_id
    }

    @ToString(includePackage = false, includeNames = true)
    @CompileStatic
    static class Tool {
        String type
        Function function
    }

    @ToString(includePackage = false, includeNames = true)
    @CompileStatic
    static class Function {
        String name
        String description
        Parameters parameters
    }

    @ToString(includePackage = false, includeNames = true)
    @CompileStatic
    static class Parameters {
        String type
        Map<String,Param> properties
        List<String> required
    }

    @ToString(includePackage = false, includeNames = true)
    @CompileStatic
    static class Param {
        String type
        String description
    }

    @ToString(includePackage = false, includeNames = true)
    @CompileStatic
    @Canonical
    static class ResponseFormat {
        static final ResponseFormat TEXT = new ResponseFormat('text')
        static final ResponseFormat JSON = new ResponseFormat('json_object')
        final String type
    }

    /**
     * ID of the model to use.
     */
    String model

    /**
     * A list of tools the model may call
     */
    List<?> messages

    List<Tool> tools

    String tool_choice

    /**
     * The maximum number of tokens that can be generated in the chat completion
     */
    Integer max_tokens

    /**
     * How many chat completion choices to generate for each input message. Note that you will be charged based on the number of generated tokens across all of the choices
     */
    Integer n

    /**
     * What sampling temperature to use, between 0 and 2
     */
    Float temperature

    /**
     * Modify the likelihood of specified tokens appearing in the completion
     */
    Map logit_bias

    /**
     * Setting to { "type": "json_object" } enables JSON mode, which guarantees the message the model generates is valid JSON.
     */
    ResponseFormat response_format
}
