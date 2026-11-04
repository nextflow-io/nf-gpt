package nextflow.gpt.prompt

import groovyx.gpars.dataflow.DataflowQueue
import nextflow.Session
import spock.lang.Requires
import spock.lang.Specification
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Requires({ System.getenv('OPENAI_API_KEY') })
class GptPromptExtensionTest extends Specification {

    def 'should run a prompt as a operator' () {
        given:
        def PROMPT = 'Extract information about a person from In 1968, amidst the fading echoes of Independence Day, a child named John arrived under the calm evening sky. This newborn, bearing the surname Doe, marked the start of a new journey.'
        def SCHEMA = [
                firstName: 'string',
                lastName: 'string',
                birthDate: 'date string (YYYY-MM-DD)'
        ]
        and:
        def session = Mock(Session) { getConfig()>>[:] }
        and:
        def ext = new GptPromptExtension(); ext.init(session)
        and:
        def source = new DataflowQueue(); source.bind(PROMPT)
        
        when:
        def ret = (DataflowQueue) ext.prompt(source, [schema:SCHEMA])
        then:
        ret.getVal() == [firstName:'John', lastName:'Doe', birthDate:'1968-07-04']
    }

    def 'should run a prompt for data as a function' () {
        given:
        def PROMPT = 'Extract information about a person from In 1968, amidst the fading echoes of Independence Day, a child named John arrived under the calm evening sky. This newborn, bearing the surname Doe, marked the start of a new journey.'
        def SCHEMA = [
                firstName: 'string',
                lastName: 'string',
                birthDate: 'date string (YYYY-MM-DD)'
        ]
        and:
        def session = Mock(Session) { getConfig()>>[:] }
        and:
        def ext = new GptPromptExtension(); ext.init(session)

        when:
        def result = ext.gptPromptForData([schema:SCHEMA], PROMPT)
        then:
        result == [ [firstName:'John', lastName:'Doe', birthDate:'1968-07-04'] ]
    }

    def 'should run a prompt for text' () {
        given:
        def PROMPT = 'Extract information about a person from In 1968, amidst the fading echoes of Independence Day, a child named John arrived under the calm evening sky. This newborn, bearing the surname Doe, marked the start of a new journey.'
        and:
        def session = Mock(Session) { getConfig()>>[:] }
        and:
        def ext = new GptPromptExtension(); ext.init(session)

        when:
        def ret = ext.gptPromptForText(PROMPT)
        then:
        ret.contains('1968')
    }

    def 'should run a prompt for text with multiple choices' () {
        given:
        def PROMPT = 'Extract information about a person from In 1968, amidst the fading echoes of Independence Day, a child named John arrived under the calm evening sky. This newborn, bearing the surname Doe, marked the start of a new journey.'
        and:
        def session = Mock(Session) { getConfig()>>[:] }
        and:
        def ext = new GptPromptExtension(); ext.init(session)

        when:
        def ret = ext.gptPromptForText(PROMPT, numOfChoices: 1)
        then:
        ret[0].contains('1968')
    }

}
