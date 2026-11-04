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

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.function.Predicate

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.failsafe.Failsafe
import dev.failsafe.RetryPolicy
import dev.failsafe.event.EventListener
import dev.failsafe.event.ExecutionAttemptedEvent
import dev.failsafe.function.CheckedSupplier
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import nextflow.gpt.config.GptConfig
import nextflow.util.Threads
/**
 * HTTP client for Gpt based API conversation
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class GptClient {

    final private String endpoint
    final private GptConfig config
    private HttpClient httpClient

    @Memoized
    static GptClient client(GptConfig config) {
        new GptClient(config)
    }

    static GptClient client() {
        return client(GptConfig.config())
    }

    /**
     * Only for testing
     */
    protected GptClient() {  }

    protected GptClient(GptConfig config) {
        this.config = config
        this.endpoint = config.endpoint()
        // create http client
        this.httpClient = newHttpClient()
    }

    protected HttpClient newHttpClient() {
        final builder = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
        // use virtual threads executor if enabled
        if( Threads.useVirtual() )
            builder.executor(Executors.newVirtualThreadPerTaskExecutor())
        // build and return the new client
        return builder.build()
    }

    GptChatCompletionResponse sendRequest(GptChatCompletionRequest request) {
        return sendRequest0(request, 1)
    }

    GptChatCompletionResponse sendRequest0(GptChatCompletionRequest request, int attempt) {
        assert endpoint, 'Missing ChatGPT endpoint'
        assert !endpoint.endsWith('/'), "Endpoint url must not end with a slash - offending value: $endpoint"
        assert config.apiKey(), "Missing ChatGPT API key"
        
        final body = new Gson().toJson(request)
        final uri = URI.create("${endpoint}/v1/chat/completions")
        log.debug "ChatGPT request: $uri; attempt=$attempt - request: $body"
        final req = HttpRequest.newBuilder()
            .uri(uri)
            .headers('Content-Type','application/json')
            .headers('Authorization', "Bearer ${config.apiKey()}")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        try {
            final resp = httpSend(req)
            log.debug "ChatGPT response: statusCode=${resp.statusCode()}; body=${resp.body()}"
            if( resp.statusCode()==200 )
                return jsonToCompletionResponse(resp.body())
            else
                throw new IllegalStateException("ChatGPT unexpected response: [${resp.statusCode()}] ${resp.body()}")
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to connect ChatGPT service: $endpoint")
        }
    }

    protected GptChatCompletionResponse jsonToCompletionResponse(String json) {
        final type = new TypeToken<GptChatCompletionResponse>(){}.getType()
        return new Gson().fromJson(json, type)
    }

    protected <T> RetryPolicy<T> retryPolicy(Predicate<? extends Throwable> cond, Predicate<T> handle) {
        final cfg = config.retryOpts()
        final listener = new EventListener<ExecutionAttemptedEvent<T>>() {
            @Override
            void accept(ExecutionAttemptedEvent<T> event) throws Throwable {
                def msg = "Gpt connection failure - attempt: ${event.attemptCount}"
                if( event.lastResult!=null )
                    msg += "; response: ${event.lastResult}"
                if( event.lastFailure != null )
                    msg += "; exception: [${event.lastFailure.class.name}] ${event.lastFailure.message}"
                log.debug(msg)
            }
        }
        return RetryPolicy.<T>builder()
                .handleIf(cond)
                .handleResultIf(handle)
                .withBackoff(cfg.delay.toMillis(), cfg.maxDelay.toMillis(), ChronoUnit.MILLIS)
                .withMaxAttempts(cfg.maxAttempts)
                .withJitter(cfg.jitter)
                .onRetry(listener)
                .build()
    }

    protected <T> HttpResponse<T> safeApply(CheckedSupplier action) {
        final retryOnException = (e -> e instanceof IOException) as Predicate<? extends Throwable>
        final retryOnStatusCode = ((HttpResponse<T> resp) -> resp.statusCode() in SERVER_ERRORS) as Predicate<HttpResponse<T>>
        final policy = retryPolicy(retryOnException, retryOnStatusCode)
        return Failsafe.with(policy).get(action)
    }

    static private final List<Integer> SERVER_ERRORS = [429,500,502,503,504]

    protected HttpResponse<String> httpSend(HttpRequest req)  {
        return safeApply(() -> httpClient.send(req, HttpResponse.BodyHandlers.ofString()))
    }
}
