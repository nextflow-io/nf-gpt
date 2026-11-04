package nextflow.processor.tip

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.Global
import nextflow.Session
import nextflow.SysEnv
import nextflow.gpt.config.GptConfig
import nextflow.gpt.prompt.GptPromptModel
import nextflow.plugin.Priority

/**
 * Implements a provider that uses Chat GPT to suggest
 * tip on task failure
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@Priority(-10)
@CompileStatic
class GptTaskTipProvider implements TaskTipProvider {

    private Session session

    GptTaskTipProvider() {
        this.session = Global.session as Session
    }

    @Override
    boolean enabled() {
        if( SysEnv.get('NXF_GPT_TIP_ENABLED','true')=='false') {
            log.debug "Env variable NXF_GPT_TIP_ENABLED=false detected - disable GPT tip provider"
            return false
        }
        final apiKey = GptConfig.config(session).apiKey()
        if (!apiKey) {
            log.debug "OpenAI API key is not available - disable GPT tip provider"
            return false
        }
        return true
    }

    @Override
    String suggestTip(List<String> context) {
        final ai = new GptPromptModel(session)
                .withResponseFormat('json_object')
                .build()
        final query = """\
I ran Nextflow but it failed with the following error. Please explain in as few words as possible.

```
${context.join('\n')}
```

"""
        final result = ai.prompt(query, [explaination: 'string'])

        return result.first().explaination
    }
}
