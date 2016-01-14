# Welcome
---

myND is a FOND (fully observable nondeterministic) and POND (partially observable nondeterministic) planner which solves planning tasks with a heuristically guided AO\* or LAO\* search.

# Compile myND
---

To compile myND, change to the source folder (*src*) and use the following command.


```
#!bash

./build
```


# Run myND
---

To run myND, change to the source folder (*src*) and use the following command.


```
#!bash

java JavaOptions mynd.MyNDPlanner PlannerOptions pathToSASFile
```

You do not have to specify explicitly if you want to solve a FOND or POND problem. The planner
determines this automatically by inspecting the version section of the SAS input file.
Most of the available planning options apply for both types of planning problems.


Useful Java Options are:

* `-Xmx4g` set the maximum heap space to 4 GB

* `-ea` enable assertions

A detailed description of the [planner options](https://bitbucket.org/robertmattmueller/mynd/src/default/wiki/Planner_Options.md) can be found on a separate Wiki page.

You can find some planning tasks in SAS+ format in the directories *data/benchmarks-fond* and *data/benchmarks-pond*.

## Example

The following command will run myND on the first blocksworld instance using LAO* search `-laostar` guided by the FF heuristic `-ff`.


```
#!bash

java -ea -Xmx4g mynd.MyNDPlanner -laostar -ff ../data/benchmarks-fond/blocksworld_p1.sas
```


# Get the plan
---

To print the plan to stdout use the `-dumpPlan` option. If you want to save the plan, redirect stdout to a file.

## Example

```
#!bash
java -ea -Xmx4g mynd.MyNDPlanner -laostar -ff -dumpPlan ../data/benchmarks-fond/blocksworld_p1.sas > plan.txt
```

If you also want to know the expected costs of the plan, use the `-calculateCosts` option.


# Translate PDDL to SAS+
---
You can find two variants of the *Fast Downward* translator in the directories *translator-fond* and *translator-pond*. We extended the translator to translate PDDL variants for FOND and POND problems. Check out the [Translator](https://bitbucket.org/robertmattmueller/mynd/src/default/wiki/Translator.md) page for details. For more information on the original translator, check out the [Fast Downward Website](http://www.fast-downward.org/).

The translator is written in Python. Here is an example of a translator call. Give the domain and problem file of the planning task as arguments.

## Example


```
#!bash

translator-fond/translate.py data/fond-pddl/blocksworld/domain.pddl data/fond-pddl/blocksworld/p1.pddl
```
As output you get a file called *output.sas* which can be parsed by myND.
