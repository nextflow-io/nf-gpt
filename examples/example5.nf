include { gptPromptForData } from 'plugin/nf-gpt'

/**
 * This example show how to perform multiple GPT prompts using combine and flatMap operators 
 */


channel
        .fromList(['Barcelona, 1992', 'London, 2012'])
        .combine(['Swimming', 'Athletics'])
        .flatMap { edition, sport ->
            gptPromptForData(
                    "Who won most gold medals in $sport category during $edition olympic games?",
                    schema: [athlete: 'string', numberOfMedals: 'number', location: 'string', sport: 'string'])
        }
        .view()
