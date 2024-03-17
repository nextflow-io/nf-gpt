package nextflow.gpt.config

import nextflow.Session
import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class GptConfigTest extends Specification {

    def 'should create default config' () {
        when:
        def config = new GptConfig([:], [OPENAI_API_KEY:'my-api-key'])
        then:
        config.endpoint() == 'https://api.openai.com'
        config.model() == 'gpt-3.5-turbo'
        config.apiKey() == 'my-api-key'
    }

    def 'should create config with custom opts' () {
        when:
        def config = new GptConfig([endpoint:'http://foo.com', model:'gpt-5', apiKey: 'xyz'], [OPENAI_API_KEY:'my-api-key'])
        then:
        config.endpoint() == 'http://foo.com'
        config.model() == 'gpt-5'
        config.apiKey() == 'xyz'
    }

    def 'should create from session' () {
        given:
        def CONFIG = [ai:[endpoint:'http://xyz.com', model:'gpt-4', apiKey: 'abc']]
        def session = Mock(Session) {getConfig()>>CONFIG  }

        when:
        def config = GptConfig.config(session)
        then:
        config.endpoint() == 'http://xyz.com'
        config.model() == 'gpt-4'
        config.apiKey() == 'abc'
    }
}
