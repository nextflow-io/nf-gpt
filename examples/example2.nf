include { prompt } from 'plugin/nf-gpt'

def query = '''
Who won most gold medals in swimming and Athletics categories during Barcelona 1992 and London 2012 olympic games?"
'''

channel .of(query)
        .prompt(schema: [athlete: 'string', numberOfMedals: 'number', location:'string', sport:'string'])
        .view()
