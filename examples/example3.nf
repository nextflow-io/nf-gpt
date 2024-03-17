include { prompt } from 'plugin/nf-ai'

channel
        .fromList(['Barcelona, 1992', 'London, 2012'])
        .combine(['Swimming', 'Athletics'])
        .prompt(schema: [athlete: 'string', numberOfMedals: 'number']) { edition, sport ->
            "Who won most gold medals in $sport category during $edition olympic games?"
        }
        .view()
