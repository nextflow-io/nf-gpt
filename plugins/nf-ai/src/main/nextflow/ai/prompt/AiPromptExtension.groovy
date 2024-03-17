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

package nextflow.ai.prompt

import static nextflow.util.CheckHelper.*

import groovy.transform.CompileStatic
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel
import nextflow.Channel
import nextflow.Session
import nextflow.extension.CH
import nextflow.extension.DataflowHelper
import nextflow.plugin.extension.Factory
import nextflow.plugin.extension.Operator
import nextflow.plugin.extension.PluginExtensionPoint
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class AiPromptExtension extends PluginExtensionPoint {

    static final private Map VALID_PROMPT_OPTS = [
        model: String,
        schema: Map,
        debug: Boolean
    ]

    private Session session

    @Override
    protected void init(Session session) {
        this.session = session
    }

    @Factory
    DataflowWriteChannel fromPrompt(Map opts, String query) {
        // check params
        checkParams( 'fromPrompt', opts, VALID_PROMPT_OPTS )
        if( opts.schema == null )
            throw new IllegalArgumentException("Missing prompt schema")
        // create the client
        final ai = new AiPromptModel(session)
            .withModel(opts.model as String)
            .withDebug(opts.debug as Boolean)
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
        checkParams( 'prompt', opts, VALID_PROMPT_OPTS )
        if( opts.schema == null )
            throw new IllegalArgumentException("Missing prompt schema")
        // create the client
        final ai = new AiPromptModel(session)
                .withModel(opts.model as String)
                .withDebug(opts.debug as Boolean)
                .build()

        final target = CH.createBy(source)
        final next = { it-> runPrompt(ai, template.call(it), opts.schema as Map, target) }
        final done = { target.bind(Channel.STOP) }
        DataflowHelper.subscribeImpl(source, [onNext: next, onComplete: done])
        return target
    }

    private void runPrompt(AiPromptModel ai, String query, Map schema, DataflowWriteChannel target) {
        // carry out the response
        final response = ai.prompt(query, schema)
        // emit the results
        for( Map<String,Object> it : response ) {
            target.bind(it)
        }
    }
}
