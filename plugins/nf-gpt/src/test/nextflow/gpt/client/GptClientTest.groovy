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

import nextflow.Session
import nextflow.gpt.config.GptConfig
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Timeout

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Timeout(30)
class GptClientTest extends Specification {

    def 'should parse response' () {
        given:
        def client = Spy(GptClient)
        and:
        def JSON = '''
{
  "id": "chatcmpl-8w6HJpPYdPLfViMbHiGUCzDXimUEC",
  "object": "chat.completion",
  "created": 1708857849,
  "model": "gpt-3.5-turbo-0125",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": null,
        "tool_calls": [
          {
            "id": "call_Erqx0Rj6JLqOnOln8AOn5lXN",
            "type": "function",
            "function": {
              "name": "get_current_weather",
              "arguments": "{\\"location\\": \\"San Francisco, CA\\"}"
            }
          },
          {
            "id": "call_nKQtiSfcHwzUYVcbvAE57kk3",
            "type": "function",
            "function": {
              "name": "get_current_weather",
              "arguments": "{\\"location\\": \\"Tokyo\\"}"
            }
          },
          {
            "id": "call_myBENYchUxjtogARuOmzG7cL",
            "type": "function",
            "function": {
              "name": "get_current_weather",
              "arguments": "{\\"location\\": \\"Paris\\"}"
            }
          }
        ]
      },
      "logprobs": null,
      "finish_reason": "tool_calls"
    }
  ],
  "usage": {
    "prompt_tokens": 80,
    "completion_tokens": 64,
    "total_tokens": 144
  },
  "system_fingerprint": "fp_86156a94a0"
}
'''

        when:
        def resp = client.jsonToCompletionResponse(JSON)
        then:
        resp.id == 'chatcmpl-8w6HJpPYdPLfViMbHiGUCzDXimUEC'
        resp.object == 'chat.completion'
        resp.created == 1708857849
        resp.model == 'gpt-3.5-turbo-0125'
        and:
        resp.choices.size() == 1
        resp.choices.get(0).index == 0
        resp.choices.get(0).finish_reason == 'tool_calls'
        resp.choices.get(0).message.role == 'assistant'
        resp.choices.get(0).message.content == null
        and:
        resp.choices.get(0).message.tool_calls.size() == 3
        and:
        resp.choices.get(0).message.tool_calls[0].type == 'function'
        resp.choices.get(0).message.tool_calls[0].function.name == 'get_current_weather'
        resp.choices.get(0).message.tool_calls[0].function.arguments == "{\"location\": \"San Francisco, CA\"}"
        and:
        resp.choices.get(0).message.tool_calls[1].type == 'function'
        resp.choices.get(0).message.tool_calls[1].function.name == 'get_current_weather'
        resp.choices.get(0).message.tool_calls[1].function.arguments == "{\"location\": \"Tokyo\"}"
        and:
        resp.choices.get(0).message.tool_calls[2].type == 'function'
        resp.choices.get(0).message.tool_calls[2].function.name == 'get_current_weather'
        resp.choices.get(0).message.tool_calls[2].function.arguments == "{\"location\": \"Paris\"}"
    }

    @Requires({ System.getenv('OPENAI_API_KEY') })
    def 'should call tools' () {
        given:
        def query = '''
                Check what's the weather like in San Francisco, Tokyo, and Paris, 
                then print the temperature for each city.
                '''.stripIndent()
        and:
        def tools = [
            new GptChatCompletionRequest.Tool(type:'function',
                function: new GptChatCompletionRequest.Function(
                    name:'get_current_weather',
                    description: 'Get the current weather in a given location',
                    parameters: new GptChatCompletionRequest.Parameters(
                        type:'object',
                        properties: [ location: new GptChatCompletionRequest.Param(type:'string',description: 'The city and state, e.g. San Francisco, CA')],
                        required: []))),

            new GptChatCompletionRequest.Tool(type:'function',
                function: new GptChatCompletionRequest.Function(
                    name:'print_value',
                    description: 'Print a generic value to the standard output',
                    parameters: new GptChatCompletionRequest.Parameters(
                        type:'object',
                        properties: [ value: new GptChatCompletionRequest.Param(type:'string', description: 'The value to be printed')],
                        required: [])))

        ]
        List<Object> messages = [ new GptChatCompletionRequest.Message(role: 'user', content: query) ]
        def request = new GptChatCompletionRequest(
            model: 'gpt-3.5-turbo-0125',
            messages: messages,
            tools: tools,
            tool_choice: 'auto' )

        and:
        def session = Mock(Session) {
            getConfig() >> [:]
        }
        and:
        def config = GptConfig.config(session)

        when:
        def response = new GptClient(config).sendRequest(request)
        then:
        response

        when:
        for( def choice : response.choices ) {
            messages << choice.message

            for( def tool : choice.message.tool_calls ) {
                messages << new GptChatCompletionRequest.ToolMessage(
                    role: 'tool',
                    name: tool.function.name,
                    content: '10',
                    tool_call_id: tool.id )
            }
        }
        and:
        def request2 = new GptChatCompletionRequest( model: 'gpt-3.5-turbo-0125', messages: messages )
        and:
        def response2 = new GptClient(config).sendRequest(request2)
        then:
        response2

    }

    @Requires({ System.getenv('OPENAI_API_KEY') })
    def 'should get structured output' () {
        given:
        def session = Mock(Session) { getConfig() >> [:] }
        def config = GptConfig.config(session)
        def client = new GptClient(config)
        and:
        def msg1 = new GptChatCompletionRequest.Message(role:'system', content:'You are a helpful assistant designed to output JSON. The top level object contains the attribute `result`. The result is a list of objects having two attributes: `year` and `location`')
        def msg2 = new GptChatCompletionRequest.Message(role:'user', content:'List of all editions of olympic games.')
        def request = new GptChatCompletionRequest(
            model: 'gpt-3.5-turbo-0125',
            messages: [msg1, msg2],
            response_format: GptChatCompletionRequest.ResponseFormat.JSON
        )

        when:
        def response = client.sendRequest(request)
        and:
        print response.choices[0].message.content
        then:
        true

    }
}
