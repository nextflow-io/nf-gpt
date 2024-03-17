include { prompt } from 'plugin/nf-ai'

def text = '''
Extract information about a person from In 1968, amidst the fading echoes of Independence Day, 
a child named John arrived under the calm evening sky. This newborn, bearing the surname Doe, 
marked the start of a new journey.
'''

channel
     .of(text)
     .prompt(schema: [firstName: 'string', lastName: 'string', birthDate: 'date (YYYY-MM-DD)'])
     .view()
