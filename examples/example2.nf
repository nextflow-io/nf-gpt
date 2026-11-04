include { gptPromptForText } from 'plugin/nf-gpt'

/*
 * This example show how to use the `gptPromptForText` function in a process
 */

process prompt {
  input:
    val query
  output:
    val response
  exec:
   response = gptPromptForText(query)
}

workflow {
    prompt('Tell me a joke') | view
}
