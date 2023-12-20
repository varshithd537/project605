# **Project Deployment Instructions**

**Cloning and Branch Selection**

**Clone the repository:**

`git clone https://github.com/varshithd537/project605.git`

Switch to the marksweep branch: `git checkout marksweep`

## **Building the Project**

Build the Garbage Collector with the specified plan:

**`bin/buildit localhost BaseBaseTutorial`**

This compiles the GC and outputs the file in the _`“dist”`_ folder.

Run the Garbage Collector with a sample HelloWorld program:
**`dist/BaseBaseTutorial_x86_64_linux/rvm -X:gc:verbose=3 HelloWorld`**

Use _`"-X:gc:verbose"`_ to enable verbose logging and observe data movement.


## **Testing with MMTk-harness**

Build MMTk Harness test tools: 
**`ant mmtk-harness`** <br>
The built jar file is located at _`“target/mmtk/“`_.

Run MMTk Harness with script files and optional arguments: <br>
**`java -jar target/mmtk/mmtk-harness.jar <script-file> [options...]`** <br>
Test script files are located in _`“MMTk/harness/test-scripts/“`_.

Example test execution:<br>

**`java -jar mmtk-harness.jar MMTk/harness/test-scripts/Alignment.script plan=MS`** <br>
Use the wrapper script for comprehensive testing: **`bin/test-mmtk [options...]`** <br>
Results are outputted in _`“results/mmtk”`_

Test against a specific plan: **`bin/test-mmtk -plan = Tutorial`**


## **Dacapo Benchmark Suite**

The Dacapo benchmark suite jar file is provided in the repository.

Usage
**`java -jar dacapo-<version>.jar [options ...] [benchmarks ...]`** <br>
Options include callback class, help, input data size, measurement methodology, and debugging.<br>
    **-c <callback>**           Use class <callback> to bracket benchmark runs <br>
    **-h**                      Print this help <br>
    **-i**                      Display benchmark information <br>
**-s small|default|large**  Size of input data <br>

Measurement methodology options <br>
**-converge**               Allow benchmark times to converge before timing <br>
    **-max_iterations <n>**     Run a max of n iterations (default 20) <br>
    **-variance <pct>**         Target coefficient of variation (default 3.0) <br>
    **-window <n>**             Measure variance over n runs (default 3) <br>
    **-n <iter>**               Run the benchmark <iter> times <br>

Debugging options (for benchmark suite maintainers) <br>
    **-debug**                  Verbose debugging information <br>
    **-ignoreValidation**       Don't halt on validation failure <br>
    **-noDigestOutput**         Turn off SHA1 digest of stdout/stderr <br>
    **-preserve**               Preserve output files (debug) <br>
    **-v**                      Verbose output <br>
    **-validationReport**       Report digests, line counts etc <br>

Example Commands

Run a benchmark with Jikes RVM: **`jikesrvm -Xmx512M -jar dacapo.jar [benchmark_name]`**  <br>
Use a specific benchmark, like _**xalan**_ : **`dist/BaseBaseTutorial_x86_64-linux/rvm -jar dacapo-2006-10.jar xalan`**


### _**For detailed commands and result screenshots, refer to the provided report.**_
