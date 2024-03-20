include { prompt } from 'plugin/nf-gpt'

channel
        .fromList(['Barcelona, 1992', 'London, 2012'])
        .combine(['Swimming', 'Athletics'])
        .prompt(schema: [athlete: 'string', numberOfMedals: 'number', location: 'string', sport: 'string']) { edition, sport ->
            "Who won most gold medals in $sport category during $edition olympic games?"
        }
        .view()
