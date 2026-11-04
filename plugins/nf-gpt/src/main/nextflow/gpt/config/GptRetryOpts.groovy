package nextflow.gpt.config

import groovy.transform.CompileStatic
import groovy.transform.ToString
import nextflow.util.Duration

@ToString(includeNames = true, includePackage = false)
@CompileStatic
class GptRetryOpts {
    Duration delay = Duration.of('450ms')
    Duration maxDelay = Duration.of('90s')
    int maxAttempts = 10
    double jitter = 0.25

    GptRetryOpts() {
        this(Collections.emptyMap())
    }

    GptRetryOpts(Map config) {
        if( config.delay )
            delay = config.delay as Duration
        if( config.maxDelay )
            maxDelay = config.maxDelay as Duration
        if( config.maxAttempts )
            maxAttempts = config.maxAttempts as int
        if( config.jitter )
            jitter = config.jitter as double
    }
}
