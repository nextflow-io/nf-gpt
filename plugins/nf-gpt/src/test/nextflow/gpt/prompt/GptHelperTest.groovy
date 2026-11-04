package nextflow.gpt.prompt

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class GptHelperTest extends Specification {

    def 'should render schema' () {
        expect:
        GptHelper.renderSchema([foo:'string']) == 'You must answer strictly in the following JSON format: {"result": [{"foo": (type: string)}] }'
    }

    def 'should render schema /1' () {
        expect:
        GptHelper.schema0([foo:'string']) == '{"foo": (type: string)}'
    }

    def 'should render schema /2' () {
        expect:
        GptHelper.schema0(SCHEMA) == EXPECTED

        where:
        SCHEMA                      | EXPECTED
        [:]                         | '{}'
        []                          | '[]'
        and:
        [color:'string',count:'integer']        | '{"color": (type: string), "count": (type: integer)}'
        [[color:'string',count:'integer']]      | '[{"color": (type: string), "count": (type: integer)}]'
        [[color:'string'], [count:'integer']]   | '[{"color": (type: string)}, {"count": (type: integer)}]'
    }

    def 'should decode response to a list' () {
        given:
        def resp
        def SCHEMA = [location:'string',year:'string']
        List<Map<String,Object>> result

        when: // a single object is given, then returns it as a list
        resp = [location: 'foo', year:'2000']
        result = GptHelper.decodeResponse0(resp, SCHEMA)
        then:
        result == [[location: 'foo', year:'2000']]

        when: // a list of location is given
        resp = [[location: 'foo', year:'2000'], [location: 'bar', year:'2001']]
        result = GptHelper.decodeResponse0(resp, SCHEMA)
        then:
        result == [[location: 'foo', year:'2000'], [location: 'bar', year:'2001']]

        when: // a list wrapped into a result object
        resp = [ games: [[location: 'foo', year:'2000'], [location: 'bar', year:'2001']] ]
        result = GptHelper.decodeResponse0(resp, SCHEMA)
        then:
        result == [[location: 'foo', year:'2000'], [location: 'bar', year:'2001']]

        when: // an indexed map is returned
        resp = [ 0: [location: 'rome', year:'2000'], 1: [location: 'barna', year:'2001'], 3: [location: 'london', year:'2002']  ]
        result = GptHelper.decodeResponse0(resp, SCHEMA)
        then:
        result == [ [location: 'rome', year:'2000'], [location: 'barna', year:'2001'], [location: 'london', year:'2002']]
    }

    def 'should check it is an index map' () {
        given:
        def SCHEMA = [a: 'string', b: 'String']
        expect:
        GptHelper.isIndexMap([0: [a: 'this', b:'that'], 1: [a: 'foo', b:'bar']], SCHEMA)
        GptHelper.isIndexMap(['0': [a: 'this', b:'that'], '1': [a: 'foo', b:'bar']], SCHEMA)
        !GptHelper.isIndexMap(['x': [a: 'this', b:'that'], 'y': [a: 'foo', b:'bar']], SCHEMA)
    }
    
    
    def 'should convert map to chat message' () {
        expect:
        GptHelper.messageToChat(List.of([role:'user', content:'this'])) == [UserMessage.from('this') ]
        GptHelper.messageToChat(List.of([role:'system', content:'that'])) == [SystemMessage.from('that') ]
        GptHelper.messageToChat(List.of([role:'ai', content:'other'])) == [AiMessage.from('other') ]
        and:
        GptHelper.messageToChat(List.of([role:'user', content:'this'],[role:'system', content:'that']))
                == [UserMessage.from('this'), SystemMessage.from('that')]

        when:
        GptHelper.messageToChat([])
        then:
        def e = thrown(IllegalArgumentException)
        e.message == 'Missing \'messages\' argument'

        when:
        GptHelper.messageToChat([[foo:'one']])
        then:
        e = thrown(IllegalArgumentException)
        e.message == 'Missing \'role\' attribute - offending message: [[foo:one]]'

        when:
        GptHelper.messageToChat([[role:'one', content:'something']])
        then:
        e = thrown(IllegalArgumentException)
        e.message == 'Unsupported message role \'one\' - offending message: [[role:one, content:something]]'

    }

}
