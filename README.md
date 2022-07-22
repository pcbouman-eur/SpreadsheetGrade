# SpreadsheetGrade

A poorly documented tool for automated assessment of spreadsheet assignments.

This repository contains a tool that takes three files: an `xml` file with an assignment specification,
an `xlsx` reference file that contains a solution to the assignment and  another `xlsx` that was handed
in by a student. It then uses the specification file to perform a number of checks on the file that was
handed in and can generate a report with feedback.

The project can be compiled using maven, with the `mvn package` command. It then produces a number of
packages in the `target` directory that can be used to perform the assessment in different ways:

* `target/XMLTestGUI-jar-with-dependencies.jar` is an executable JAR file that contains a Swing GUI, 
  which was meant to let a teaching assistant develop and test the assignments easily.
* `target/XMLTest-jar-with-dependencies.jar` is an executable JAR file with a basic command line application that
  generates output in a feedback style inspired by the Autolab software from Carnegie Mellon University.
* `target/checkqf-jar-with-dependencies.jar` is an executable JAR file with a picocli based command line
  interface that generates the feedback as a `json` file that can hopefully be understood by Quarterfall.
* `target/excelgrader-jar-with-dependencies.jar` is meant to be used with another process, and performs
  Inter Process Communication via stdin/stdout. It is not very useful for regular users, and should be
  ignored.
  
## Quick demo

Assuming a modern enough version of Java (>= 8) and Maven are installed, the following steps can be used
to test this:

```
mvn package
```

A demo assignment can be found in `/example`. 

### Running the GUI

Running the GUI can then be done with:

```
java -jar target/XMLTestGUI-jar-with-dependencies.jar
```

And selected the proper files from the `/example` directory.

### Running the Autolab based CLI

Running the Autolab inspired CLI can be done using:

```
java -jar target/XMLTest-jar-with-dependencies.jar example/example.xml example/reference.xlsx example/handin.xlsx
```

which then generates the following report if everything works out:

```
Results for exercise Interesting Interests

****************
* PASSED TESTS *
****************
[   8 out of   8 ] : Computation of the ending budget (all combinations of extreme values)
[ 256 out of 256 ] : Computation of the ending budget (random values)
[   8 out of   8 ] : Computation of the profit (all combinations of extreme values)
[ 256 out of 256 ] : Computation of the profit (random values)
[   1 out of   2 ] : References to cell B4 are absolute
[   1 out of   1 ] : The function 'MIN' is never used

****************
* FAILED TESTS *
****************
[   4 out of   4 ] : Can you deal with zero periods? (all combinations of extreme values)
[ 256 out of 256 ] : Can you deal with zero periods? (random values)
[   2 out of   2 ] : References to cell B3 are absolute
[   1 out of   2 ] : References to cell B4 are absolute
[   1 out of   1 ] : References to column C are absolute
[   2 out of   2 ] : References to row 2 are absolute
[   1 out of   1 ] : The function 'MAX' is used at least once
```

### Running the Quarterfall based CLI

Running the Quarterfall based CLI can be done as follows:

```
java -jar target/checkqf-jar-with-dependencies.jar
```

which prints the standard help text:

```
Missing required options and parameters: '--spec=<exerciseSpec>', '--ref=<reference>', '<checkFile>'
Usage: checkqf [-cdhV] [-o=<output>] -r=<reference> -s=<exerciseSpec>
               <checkFile>
Spreadsheet checker with Quarterfall output
      <checkFile>         The file to check and grade
  -c, --clean             Writes a new file even if the .json output already
                            exists
  -d, --dense             Writes a dense, non-indented json representation
  -h, --help              Show this help message and exit.
  -o, --output=<output>   Output .json file to write feedback to
  -r, --ref=<reference>   Reference .xlsx file with the correct solution
  -s, --spec=<exerciseSpec>
                          XML file with the specification of the exercise
  -V, --version           Print version information and exit.
```

Running it one the demo files can be done as follows:

```
java -jar target/checkqf-jar-with-dependencies.jar -s example/example.xml -r example/reference.xlsx -o output.json example/handin.xlsx
```

which modifies/creates the file `output.json`. If the file already exists, a `feedback` property is added to the existing object.

If no file exists, the following `json` file is generated:
```
{
  "feedback" : [ "# Passed Tests\n\n* ✔ [   8 out of   8 ] : Computation of the ending budget (all combinations of extreme values)\n* ✔ [ 256 out of 256 ] : Computation of the ending budget (random values)\n* ✔ [   8 out of   8 ] : Computation of the profit (all combinations of extreme values)\n* ✔ [ 256 out of 256 ] : Computation of the profit (random values)\n* ✔ [   1 out of   2 ] : References to cell B4 are absolute\n* ✔ [   1 out of   1 ] : The function 'MIN' is never used\n", "# Failed Tests\n\n* ❌ [   4 out of   4 ] : Can you deal with zero periods? (all combinations of extreme values)\n* ❌ [ 256 out of 256 ] : Can you deal with zero periods? (random values)\n* ❌ [   2 out of   2 ] : References to cell B3 are absolute\n* ❌ [   1 out of   2 ] : References to cell B4 are absolute\n* ❌ [   1 out of   1 ] : References to column C are absolute\n* ❌ [   2 out of   2 ] : References to row 2 are absolute\n* ❌ [   1 out of   1 ] : The function 'MAX' is used at least once\n" ]
}
```

## XML Specification

The XML schema is available as `/schema/spreadsheettest.xsd`, but it is not documented, and things
were implemented somewhere in 2016 in a rush.
There are a number of Excel-specific features in the tool, such as checking if certain functions are used,
checking if particular named ranges exist, and changing data and reevaluating both sheets,
but few of them are battle tested.
Most of the assignments we use in practice just do cell-to-cell comparisons between the reference
and hand-in spreadsheets.
