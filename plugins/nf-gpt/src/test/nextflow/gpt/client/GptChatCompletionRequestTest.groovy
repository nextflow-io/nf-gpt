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

import groovy.json.JsonOutput
import spock.lang.Specification
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class GptChatCompletionRequestTest extends Specification {

    def 'should serialize a request' () {
        given:
        def p1 = new GptChatCompletionRequest.Param(type: 'string', description: 'Foo')
        def p2 = new GptChatCompletionRequest.Param(type: 'string', description: 'Foo')
        def parameters =new GptChatCompletionRequest.Parameters(type: 'object', properties: ['p1': p1, 'p2':p2], required: ['none'])
        def fun = new GptChatCompletionRequest.Function(name: 'whats_the_weather_like', description: 'Just a description', parameters: parameters)
        def tool = new GptChatCompletionRequest.Tool(type:'function', function: fun)
        def msg = new GptChatCompletionRequest.Message(role: 'user', content: 'How do you do?')
        and:
        def request = new GptChatCompletionRequest(model: 'turbo', messages: [msg], tools: [tool])
        when:
        def json = JsonOutput.prettyPrint(JsonOutput.toJson(request))
        then:
        json == '''\
            {
                "model": "turbo",
                "messages": [
                    {
                        "role": "user",
                        "content": "How do you do?"
                    }
                ],
                "tools": [
                    {
                        "type": "function",
                        "function": {
                            "name": "whats_the_weather_like",
                            "description": "Just a description",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "p1": {
                                        "type": "string",
                                        "description": "Foo"
                                    },
                                    "p2": {
                                        "type": "string",
                                        "description": "Foo"
                                    }
                                },
                                "required": [
                                    "none"
                                ]
                            }
                        }
                    }
                ],
                "tool_choice": null,
                "max_tokens": null,
                "n": null,
                "temperature": null,
                "logit_bias": null,
                "response_format": null
            }
            '''.stripIndent().rightTrim()

    }
}
