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

import nextflow.Session
import spock.lang.Requires
import spock.lang.Specification
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class GptPromptModelTest extends Specification {

    def 'should render schema' () {
        expect:
        GptPromptModel.renderSchema([foo:'string']) == 'You must answer strictly in the following JSON format: {"result": [{"foo": (type: string)}] }'
    }

    def 'should render schema /1' () {
        expect:
        GptPromptModel.schema0([foo:'string']) == '{"foo": (type: string)}'
    }

    def 'should render schema /2' () {
        expect:
        GptPromptModel.schema0(SCHEMA) == EXPECTED

        where:
        SCHEMA                      | EXPECTED
        [:]                         | '{}'
        []                          | '[]'
        and:
        [color:'string',count:'integer']        | '{"color": (type: string), "count": (type: integer)}'
        [[color:'string',count:'integer']]      | '[{"color": (type: string), "count": (type: integer)}]'
        [[color:'string'], [count:'integer']]   | '[{"color": (type: string)}, {"count": (type: integer)}]'
    }

    @Requires({ System.getenv('OPENAI_API_KEY') })
    def 'should render json response' () {
        given:
        def PROMPT = 'Extract information about a person from In 1968, amidst the fading echoes of Independence Day, a child named John arrived under the calm evening sky. This newborn, bearing the surname Doe, marked the start of a new journey.'
        def SCHEMA = [
            firstName: 'string',
            lastName: 'string',
            birthDate: 'date string (YYYY-MM-DD)'
        ]
        and:
        def session = Mock(Session) { getConfig()>>[:] }
        def model = new GptPromptModel(session).build()

        when:
        def result = model.prompt(PROMPT, SCHEMA)
        then:
        result[0].firstName == "John"
        result[0].lastName == "Doe"
        result[0].birthDate == '1968-07-04'
    }

    def 'should decode response to a list' () {
        given:
        def resp
        def SCHEMA = [location:'string',year:'string']
        List<Map<String,Object>> result

        when: // a single object is given, then returns it as a list
        resp = [location: 'foo', year:'2000']
        result = GptPromptModel.decodeResponse0(resp, SCHEMA)
        then:
        result == [[location: 'foo', year:'2000']]

        when: // a list of location is given
        resp = [[location: 'foo', year:'2000'], [location: 'bar', year:'2001']]
        result = GptPromptModel.decodeResponse0(resp, SCHEMA)
        then:
        result == [[location: 'foo', year:'2000'], [location: 'bar', year:'2001']]

        when: // a list wrapped into a result object
        resp = [ games: [[location: 'foo', year:'2000'], [location: 'bar', year:'2001']] ]
        result = GptPromptModel.decodeResponse0(resp, SCHEMA)
        then:
        result == [[location: 'foo', year:'2000'], [location: 'bar', year:'2001']]

        when: // an indexed map is returned
        resp = [ 0: [location: 'rome', year:'2000'], 1: [location: 'barna', year:'2001'], 3: [location: 'london', year:'2002']  ]
        result = GptPromptModel.decodeResponse0(resp, SCHEMA)
        then:
        result == [ [location: 'rome', year:'2000'], [location: 'barna', year:'2001'], [location: 'london', year:'2002']]
    }

    def 'should check it is an index map' () {
        given:
        def SCHEMA = [a: 'string', b: 'String']
        expect:
        GptPromptModel.isIndexMap([0: [a: 'this', b:'that'], 1: [a: 'foo', b:'bar']], SCHEMA)
        GptPromptModel.isIndexMap(['0': [a: 'this', b:'that'], '1': [a: 'foo', b:'bar']], SCHEMA)
        !GptPromptModel.isIndexMap(['x': [a: 'this', b:'that'], 'y': [a: 'foo', b:'bar']], SCHEMA)
    }

}
