# CE316 Integrated Assignment Environment (IAE)
# CE316-course-project

## IMPORTANT

The repository includes ready-to-use demo materials under:

test-submissions-2/

This folder contains:

* Sample student ZIP submissions for Java, Python, C, C++, and Haskell
* Sample input files
* Sample expected output files

For evaluation and demonstration purposes, it is recommended to use the files included in the test-submissions-2 folder.

---

## HOW TO RUN

Install the application using:

IAE-Setup-1.0.0.exe

After installation, launch the application from the desktop shortcut or Start Menu.

---

## REQUIRED TOOLS FOR STUDENT EVALUATION

The application itself can run without additional setup.

To compile and evaluate student submissions, the required compiler/interpreter must be available on the system PATH.

### Java

No additional installation is required.

### Python

Install Python 3:

https://python.org/downloads

Enable "Add Python to PATH" during installation.

### C / C++

Install MinGW-w64:

https://winlibs.com

Add the MinGW bin directory to the system PATH.

### Haskell

Install GHC using GHCup:

https://www.haskell.org/ghcup/

Follow the installation instructions on the website.

---

## INCLUDED DEMO MATERIALS

The test-submissions-2 folder contains all files required to test the application.

Structure:

* c/
* cpp/
* haskell/
* java/
* python/
* test-cases/

The language folders contain sample student ZIP submissions.

The test-cases folder contains sample input and expected output files that can be selected directly during project creation.

---

## CONFIGURATION MANAGEMENT

The application supports:

* Creating configurations
* Editing configurations
* Deleting configurations
* Importing configurations
* Exporting configurations

Default configurations are provided for:

* Java
* Python
* C
* Haskell

Additional language configurations can be created through the Configuration Management screen.

### Example C++ Configuration

C++ is intentionally not included in the default configurations and can be added through the Configuration Management screen.

Example values:

Name:
C++ Config

Compile Command:
g++ *.cpp -o main

Run Command:
./main

Source Extension:
.cpp

Entry Pattern:
int\s+main

---

## CREATING A PROJECT

1. Open the application.
2. Select Create Project.
3. Enter a project name.
4. Select a configuration.
5. Add one or more Input / Expected Output pairs.
6. Select the Student Submissions folder.
7. Start the evaluation.

### Selecting Test Cases

Input and Expected Output files should be selected from:

test-submissions-2/test-cases/

### Selecting Student Submissions

If a specific configuration is selected, choose the matching submission folder:

* C → test-submissions-2/c
* C++ → test-submissions-2/cpp
* Java → test-submissions-2/java
* Python → test-submissions-2/python
* Haskell → test-submissions-2/haskell

The application will automatically:

* Extract ZIP submissions
* Compile or interpret source code
* Execute student programs
* Compare outputs with expected outputs
* Save evaluation results

---

## EVALUATION RESULTS

The Evaluation Results screen provides:

* Total student count
* Pass / Fail statistics
* Compile status
* Run status
* Output comparison status
* Final evaluation result
* Pass rate information

Detailed results can be viewed for each student submission.

---

## STUDENT DETAILS

The Student Details screen displays information such as:

* Student ID
* Source files
* Compilation result and exit code
* Compiler messages
* Expected output
* Actual output
* Output comparison results
* Evaluation status (Passed / Failed)
* Individual test case results

---

## PROJECT DETAILS

The Project Details screen displays:

* Selected configuration
* Project creation date
* Input file information
* Expected output file information
* Student submissions folder
* Evaluation summary
* Pass/fail statistics

Projects can be re-evaluated using the Run Again button.

---

## PROJECT MANAGEMENT

Projects can be:

* Created
* Saved
* Reopened
* Re-evaluated
* Exported

All project information and evaluation results are stored locally using SQLite.

---

## HELP

The application includes a built-in Help and Manual screen accessible from the Help menu.

---

## FEATURES

* Create, edit, delete, import, and export configurations
* Create and manage evaluation projects
* Process multiple student ZIP submissions
* Compile and execute submissions
* Compare outputs against expected outputs
* View detailed evaluation results
* View detailed student information
* Save and reopen projects
* Re-run evaluations
* Real-time progress tracking
* Evaluation cancellation support
* Built-in Help documentation
* SQLite-based persistence
* Windows installer support
