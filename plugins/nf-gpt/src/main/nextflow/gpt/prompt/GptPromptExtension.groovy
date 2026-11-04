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


import static nextflow.util.CheckHelper.*

import groovy.transform.CompileStatic
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel
import nextflow.Channel
import nextflow.Session
import nextflow.extension.CH
import nextflow.extension.DataflowHelper
import nextflow.gpt.client.GptChatCompletionRequest
import nextflow.gpt.client.GptClient
import nextflow.gpt.config.GptConfig
import nextflow.plugin.extension.Factory
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.Operator
import nextflow.plugin.extension.PluginExtensionPoint
/**
 * Implements GPT Chat extension methods
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class GptPromptExtension extends PluginExtensionPoint {

    static final private Map VALID_PROMPT_DATA_OPTS = [
        model: String,
        schema: Map,
        debug: Boolean,
        temperature: Double,
        maxTokens: Integer
    ]

    static final private Map VALID_PROMPT_TEXT_OPTS = [
            model: String,
            debug: Boolean,
            temperature: Double,
            maxTokens: Integer,
            numOfChoices: Integer,
            logitBias: Map
    ]

    private Session session

    @Override
    protected void init(Session session) {
        this.session = session
    }

    @Factory
    DataflowWriteChannel fromPrompt(Map opts, String query) {
        // check params
        checkParams( 'fromPrompt', opts, VALID_PROMPT_DATA_OPTS )
        if( opts.schema == null )
            throw new IllegalArgumentException("Missing prompt schema")
        // create the client
        final ai = new GptPromptModel(session)
            .withModel(opts.model as String)
            .withDebug(opts.debug as Boolean)
            .withTemperature(opts.temperature as Double)
            .withMaxToken(opts.maxTokens as Integer)
            .withJsonResponseFormat()
            .build()
        // run the prompt
        final response = ai.prompt(query, opts.schema as Map)
        final target = CH.create()
        CH.emitAndClose(target, response)
        return target
    }

    @Operator
    DataflowWriteChannel prompt(DataflowReadChannel source, Map opts) {
        prompt(source, opts, it-> it.toString())
    }

    @Operator
    DataflowWriteChannel prompt(DataflowReadChannel source, Map opts, Closure<String> template) {
        // check params
        checkParams( 'prompt', opts, VALID_PROMPT_DATA_OPTS )
        if( opts.schema == null )
            throw new IllegalArgumentException("Missing prompt schema")
        // create the client
        final ai = new GptPromptModel(session)
                .withModel(opts.model as String)
                .withDebug(opts.debug as Boolean)
                .withTemperature(opts.temperature as Double)
                .withMaxToken(opts.maxTokens as Integer)
                .withJsonResponseFormat()
                .build()

        final target = CH.createBy(source)
        final next = { it-> runPrompt(ai, template.call(it), opts.schema as Map, target) }
        final done = { target.bind(Channel.STOP) }
        DataflowHelper.subscribeImpl(source, [onNext: next, onComplete: done])
        return target
    }

    private void runPrompt(GptPromptModel ai, String query, Map schema, DataflowWriteChannel target) {
        // carry out the response
        final response = ai.prompt(query, schema)
        // emit the results
        for( Map<String,Object> it : response ) {
            target.bind(it)
        }
    }

    @Function
    List<Map<String,Object>> gptPromptForData(Map opts, CharSequence query) {
        // check params
        checkParams( 'gptPromptForData', opts, VALID_PROMPT_DATA_OPTS )
        if( opts.schema == null )
            throw new IllegalArgumentException("Missing prompt schema")
        // create the client
        final ai = new GptPromptModel(session)
                .withModel(opts.model as String)
                .withDebug(opts.debug as Boolean)
                .withTemperature(opts.temperature as Double)
                .withMaxToken(opts.maxTokens as Integer)
                .withJsonResponseFormat()
                .build()

        return ai.prompt(query.toString(), opts.schema as Map)
    }

    /**
     * Carry out a GPT text prompt providing one or more messages
     *
     * @param opts
     *      Hold the prompt options
     * @param messages
     *      The prompt message content
     * @return
     *      The response content as a string or a list of string when the {@code numOfChoices} option is specified
     */
    @Function
    Object gptPromptForText(Map opts=Map.of(), String message) {
        gptPromptForText(opts, List.of(Map.of('role','user', 'content',message)))
    }

    /**
     * Carry out a GPT text prompt providing one or more messages
     *
     * @param opts
     *      Hold the prompt options
     * @param messages
     *      Hold the messages to carry out the prompt provided a list of key-value pairs, where the key represent
     *      the message "role" and the value thr message content e.g.
     *      {@code [ [system: "You should act as a good guy"], [role: "Tell me a joke"] ]
     * @return
     *      The response content as a string or a list of string when the {@code numOfChoices} option is specified
     */
    @Function
    Object gptPromptForText(Map opts=Map.of(), List<Map<String,String>> messages) {
        // check params
        checkParams( 'gptPromptForText', opts, VALID_PROMPT_TEXT_OPTS )

        final config = GptConfig.config(session)
        final client = GptClient.client(config)
        final model = opts.model ?: config.model()
        final numOfChoices = opts.numOfChoices as Integer ?: 1
        final temperature = opts.temperature as Double ?: config.temperature()
        final msg = messages.collect ((Map it)-> new GptChatCompletionRequest.Message(role:it.role, content:it.content))
        final request = new GptChatCompletionRequest(
                model: model,
                temperature: temperature,
                messages: msg,
                n: numOfChoices,
                max_tokens: opts.maxTokens as Integer,
                logit_bias: opts.logitBias as Map
        )
        final resp = client.sendRequest(request)
        return opts.numOfChoices==null
                ? resp.choices.get(0).message.content
                : resp.choices.collect(it-> it.message.content)
    }

}
