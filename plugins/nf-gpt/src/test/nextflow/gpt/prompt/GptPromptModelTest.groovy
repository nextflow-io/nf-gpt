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
        def model = new GptPromptModel(session).withJsonResponseFormat().build()

        when:
        def result = model.prompt(PROMPT, SCHEMA)
        then:
        result[0].firstName == "John"
        result[0].lastName == "Doe"
        result[0].birthDate == '1968-07-04'
    }



}
