include { gptPromptForText } from 'plugin/nf-gpt'

/*
 * This example show how to use the `gptPromptForText` function in the map operator
 */

channel
     .of('Tell me joke')
     .map { gptPromptForText(it) }
     .view()
