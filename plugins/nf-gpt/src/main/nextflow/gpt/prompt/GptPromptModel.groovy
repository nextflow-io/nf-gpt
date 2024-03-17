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

package nextflow.gpt.prompt

import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.openai.OpenAiChatModel
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.Session
import nextflow.gpt.config.GptConfig
import nextflow.util.StringUtils
/**
 * Simple AI client for OpenAI model
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class GptPromptModel {

    private GptConfig config
    private OpenAiChatModel client

    private String model
    private boolean debug

    GptPromptModel(Session session) {
        this.config = GptConfig.config(session)
    }

    GptPromptModel withModel(String model) {
        this.model = model
        return this
    }

    GptPromptModel withDebug(Boolean value) {
        this.debug = value
        return this
    }

    GptPromptModel build() {
        final modelName = model ?: config.model()
        log.debug "Creating OpenAI chat model: $modelName; api-key: ${StringUtils.redact(config.apiKey())}"
        client = OpenAiChatModel.builder()
            .apiKey(config.apiKey())
            .modelName(modelName)
            .logRequests(debug)
            .logResponses(debug)
            .responseFormat("json_object")
            .build();
        return this
    }

    List<Map<String,Object>> prompt(String query, Map schema) {
        if( !query )
            throw new IllegalArgumentException("Missing AI prompt")
        final content =  query + '. ' + renderSchema(schema)
        final msg = UserMessage.from(content)
        if( debug )
            log.debug "AI message: $msg"
        final json = client.generate(List.<ChatMessage>of(msg)).content().text()
        if( debug )
            log.debug "AI response: $json"
        return decodeResponse(new JsonSlurper().parseText(json), schema)
    }

    static protected String renderSchema(Map schema) {
        return 'You must answer strictly in the following JSON format: {"result": [' + schema0(schema) + '] }'
    }

    static protected String schema0(Object schema) {
        if( schema instanceof List ) {
            return "[" + (schema as List).collect(it -> schema0(it)).join(', ') + "]"
        }
        else if( schema instanceof Map ) {
            return "{" + (schema as Map).collect( it -> "\"$it.key\": " + schema0(it.value) ).join(', ') + "}"
        }
        else if( schema instanceof CharSequence ) {
            return "(type: $schema)"
        }
        else if( schema != null )
            throw new IllegalArgumentException("Unexpected data type: ")
        else
            throw new IllegalArgumentException("Data structure cannot be null")
    }

    static protected List<Map<String,Object>> decodeResponse(Object response, Map schema) {
        final result = decodeResponse0(response,schema)
        if( !result )
            throw new IllegalArgumentException("Response does not match expected schema: $schema - Offending value: $response")
        return result
    }

    static protected List<Map<String,Object>> decodeResponse0(Object response, Map schema) {
        final expected = schema.keySet()
        if( response instanceof Map ) {
            if( response.keySet()==expected ) {
                return List.of(response as Map<String,Object>)
            }
            if( isIndexMap(response, schema) ) {
                return new ArrayList<Map<String, Object>>(response.values() as Collection<Map<String,Object>>)
            }
            if( response.size()==1 ) {
                return decodeResponse(response.values().first(), schema)
            }
        }

        if( response instanceof List ) {
            final it = (response as List).first()
            if( it instanceof Map && it.keySet()==expected )
                return response as List<Map<String,Object>>
        }
        return null
    }

    static protected boolean isIndexMap(Map response, Map schema) {
        final keys = response.keySet()
        // check all key are integers e.g. 0, 1, 2
        if( keys.every(it-> it.toString().isInteger() ) ) {
            // take the first and check the object matches the scherma
            final it = response.values().first()
            return it instanceof Map && it.keySet()==schema.keySet()
        }
        return false
    }
}
