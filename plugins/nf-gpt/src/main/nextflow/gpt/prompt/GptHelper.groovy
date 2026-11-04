package nextflow.gpt.prompt

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage

/**
 * Helper methods for GPT conversation
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class GptHelper {

    static protected String renderSchema(Map schema) {
        return 'You must answer strictly in the following JSON format: {"result": [' + schema0(schema) + '] }'
    }

    static protected String schema0(Object schema) {
        if( schema instanceof List ) {
            return "[" + (schema as List).collect(it -> schema0(it)).join(', ') + "]"
        }
        else if( schema instanceof Map ) {
            return "{" + (schema as Map).collect( it -> "\"$it.key\": " + schema0(it.value) ).join(', ') + "}"
        }
        else if( schema instanceof CharSequence ) {
            return "(type: $schema)"
        }
        else if( schema != null )
            throw new IllegalArgumentException("Unexpected data type: ")
        else
            throw new IllegalArgumentException("Data structure cannot be null")
    }

    static protected List<Map<String,Object>> decodeResponse(Object response, Map schema) {
        final result = decodeResponse0(response,schema)
        if( !result )
            throw new IllegalArgumentException("Response does not match expected schema: $schema - Offending value: $response")
        return result
    }

    static protected List<Map<String,Object>> decodeResponse0(Object response, Map schema) {
        final expected = schema.keySet()
        if( response instanceof Map ) {
            if( response.keySet()==expected ) {
                return List.of(response as Map<String,Object>)
            }
            if( isIndexMap(response, schema) ) {
                return new ArrayList<Map<String, Object>>(response.values() as Collection<Map<String,Object>>)
            }
            if( response.size()==1 ) {
                return decodeResponse(response.values().first(), schema)
            }
        }

        if( response instanceof List ) {
            final it = (response as List).first()
            if( it instanceof Map && it.keySet()==expected )
                return response as List<Map<String,Object>>
        }
        return null
    }

    static protected boolean isIndexMap(Map response, Map schema) {
        final keys = response.keySet()
        // check all key are integers e.g. 0, 1, 2
        if( keys.every(it-> it.toString().isInteger() ) ) {
            // take the first and check the object matches the scherma
            final it = response.values().first()
            return it instanceof Map && it.keySet()==schema.keySet()
        }
        return false
    }

    static List<ChatMessage> messageToChat(List<Map<String,String>> messages) {
        if( !messages )
            throw new IllegalArgumentException("Missing 'messages' argument")
        final result = new ArrayList<ChatMessage> ()
        for( Map<String,String> it : messages ) {
            if( !it.role )
                throw new IllegalArgumentException("Missing 'role' attribute - offending message: $messages")
            if( !it.content )
                throw new IllegalArgumentException("Missing 'content' attribute - offending message: $messages")
            final msg = switch (it.role) {
                case 'user' -> UserMessage.from(it.content)
                case 'system' -> SystemMessage.from(it.content)
                case 'ai' -> AiMessage.from(it.content)
                default -> throw new IllegalArgumentException("Unsupported message role '${it.role}' - offending message: $messages")
            }
            result.add(msg)
        }
        return result
    }

}
