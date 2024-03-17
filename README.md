# nf-gpt plugin
 
nf-gpt is an experimental plugin to integrate GPT prompts into Nextflow scripts. It allows submitting
prompts via OpenAI API and collect the response in form of structured data for downstream analysis.

## Get started

1. Configure the [OpenAI API](https://platform.openai.com/api-keys) key in your environment by using the following variable:

```bash
export OPENAI_API_KEY=<your api key>
```

2. Add the following snippet at the beginning of your script:

```nextflow
include { prompt } from 'plugin/nf-gpt'
```

3. Use the `prompt` operator to perform a ChatGPT query and collect teh result to a map object having the schema
of your choice, e.g.

```
include { prompt } from 'plugin/nf-gpt'

def text = '''
Extract information about a person from In 1968, amidst the fading echoes of Independence Day,
a child named John arrived under the calm evening sky. This newborn, bearing the surname Doe,
marked the start of a new journey.
'''

channel
     .of(text)
     .prompt(schema: [firstName: 'string', lastName: 'string', birthDate: 'date (YYYY-MM-DD)'])
     .view()
```

4. run using nextflow as usual

```
nextflow run <my script>
```

### Other example

See the folder [examples] for more examples


## Testing and debugging

To build and test the plugin during development, configure a local Nextflow build with the following steps:

1. Clone the Nextflow repository in your computer into a sibling directory:
    ```bash
    git clone --depth 1 https://github.com/nextflow-io/nextflow ../nextflow
    ```
  
2. Configure the plugin build to use the local Nextflow code:
    ```bash
    echo "includeBuild('../nextflow')" >> settings.gradle
    ```
  
   (Make sure to not add it more than once!)

3. Compile the plugin alongside the Nextflow code:
    ```bash
    make assemble
    ```

4. Run Nextflow with the plugin, using `./launch.sh` as a drop-in replacement for the `nextflow` command, and adding the option `-plugins nf-gpt` to load the plugin:
    ```bash
    ./launch.sh run nextflow-io/hello -plugins nf-gpt
    ```

## Testing without Nextflow build

The plugin can be tested without using a local Nextflow build using the following steps:

1. Build the plugin: `make buildPlugins`
2. Copy `build/plugins/<your-plugin>` to `$HOME/.nextflow/plugins`
3. Create a pipeline that uses your plugin and run it: `nextflow run ./my-pipeline-script.nf`

## Package, upload, and publish

The project should be hosted in a GitHub repository whose name matches the name of the plugin, that is the name of the directory in the `plugins` folder (e.g. `nf-gpt`).

Follow these steps to package, upload and publish the plugin:

1. Create a file named `gradle.properties` in the project root containing the following attributes (this file should not be committed to Git):

   * `github_organization`: the GitHub organisation where the plugin repository is hosted.
   * `github_username`: The GitHub username granting access to the plugin repository.
   * `github_access_token`: The GitHub access token required to upload and commit changes to the plugin repository.
   * `github_commit_email`: The email address associated with your GitHub account.

2. Use the following command to package and create a release for your plugin on GitHub:
    ```bash
    ./gradlew :plugins:nf-gpt:upload
    ```

3. Create a pull request against [nextflow-io/plugins](https://github.com/nextflow-io/plugins/blob/main/plugins.json) to make the plugin accessible to Nextflow.
