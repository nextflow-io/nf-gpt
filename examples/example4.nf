include { gptPromptForData } from 'plugin/nf-gpt'

/**
 * This example show how to perform a GPT prompt and map the response to a structured object
 */


def query = '''
Who won most gold medals in swimming and Athletics categories during Barcelona 1992 and London 2012 olympic games?"
'''

def RECORD = [athlete: 'string', numberOfMedals: 'number', location:'string', sport:'string']

channel .of(query)
        .flatMap { gptPromptForData(it, schema:RECORD, temperature: 2d) }
        .view()
