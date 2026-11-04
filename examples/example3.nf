include { gptPromptForData } from 'plugin/nf-gpt'

/**
 * This example show how to perform a GPT prompt and map the response to a structured object
 */

def text = '''
Extract information about a person from In 1968, amidst the fading echoes of Independence Day, 
a child named John arrived under the calm evening sky. This newborn, bearing the surname Doe, 
marked the start of a new journey.
'''

channel
     .of(text)
     .flatMap { gptPromptForData(it, schema: [firstName: 'string', lastName: 'string', birthDate: 'date (YYYY-MM-DD)']) }
     .view()
