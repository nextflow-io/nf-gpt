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
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.openai.OpenAiChatModel
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.Session
import nextflow.gpt.config.GptConfig
import nextflow.util.StringUtils

import static nextflow.gpt.prompt.GptHelper.*

/**
 * Simple AI client for OpenAI model
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class GptPromptModel {

    private static final String JSON_OBJECT = "json_object"

    private GptConfig config
    private OpenAiChatModel client

    private String model
    private boolean debug
    private Double temperature
    private Integer maxTokens
    private String responseFormat

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

    GptPromptModel withTemperature(Double d) {
        this.temperature = d
        return this
    }

    GptPromptModel withMaxToken(Integer i) {
        this.maxTokens = i
        return this
    }

    GptPromptModel withResponseFormat(String format) {
        this.responseFormat = format
        return this
    }

    GptPromptModel withJsonResponseFormat() {
        this.responseFormat = JSON_OBJECT
        return this
    }

    GptPromptModel build() {
        final modelName = model ?: config.model()
        final temperature = this.temperature ?: config.temperature()
        final tokens = maxTokens ?: config.maxTokens()
        log.debug "Creating OpenAI chat model: $modelName; api-key: ${StringUtils.redact(config.apiKey())}; temperature: $temperature; maxTokens: ${maxTokens}"
        client = OpenAiChatModel.builder()
            .apiKey(config.apiKey())
            .modelName(modelName)
            .logRequests(debug)
            .logResponses(debug)
            .temperature(temperature)
            .maxTokens(tokens)
            .responseFormat(responseFormat)
            .build();
        return this
    }

    List<Map<String,Object>> prompt(List<ChatMessage> messages, Map schema) {
        if( !messages )
            throw new IllegalArgumentException("Missing AI prompt")
        if( !schema )
            throw new IllegalArgumentException("Missing AI prompt schema")
        if( responseFormat!=JSON_OBJECT )
            throw new IllegalStateException("AI prompt requires json_object response format")
        final all = new ArrayList(messages)
        all.add(SystemMessage.from(renderSchema(schema)))
        if( debug )
            log.debug "AI message: $all"
        final json = client.generate(all).content().text()
        if( debug )
            log.debug "AI response: $json"
        return decodeResponse(new JsonSlurper().parseText(json), schema)
    }

    List<Map<String,Object>> prompt(String query, Map schema) {
        if( !query )
            throw new IllegalArgumentException("Missing AI prompt")
        final msg = UserMessage.from(query)
        return prompt(List.<ChatMessage>of(msg), schema)
    }

    String generate(List<ChatMessage> messages) {
        if( responseFormat )
            throw new IllegalArgumentException("Response format '$responseFormat' not support by 'generate' function")
        return client.generate(messages).content().text()
    }
}
