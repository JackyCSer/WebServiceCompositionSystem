# Translator
---

There are two different translators in the repository: A **FOND** (fully observable nondeterministic) and a **POND** (partially observable nondeterministic) version, both based on the [Fast Downward](http://www.fast-downward.org/) translator for classical planning tasks. They are used to transform planning tasks from a PDDL-like description to a SAS+-like description. The output of the translation is used as input for myND.

# FOND translator
---

## Input

myND supports a limited PDDL variant extended by a `oneof` keyword to describe nondeterministic operators.

### Limitations

Please check out the [Fast Downward Website](http://www.fast-downward.org/PddlSupport) for information on PDDL support. Furthermore there are the following limitations specific to myND:

* Conditional effects are not supported.
* Universal conditions in effects are not supported.

### Example of using oneof

This is an example from the blocksworld domain. After picking-up *block b1*, b1 is either in the hand of the agent or b1 is on the table.
```
#!pddl

:effect
       (oneof 
           (and 
               (holding ?b1)
               (clear ?b2)
               (not (emptyhand)) 
               (not (clear ?b1))
               (not (on ?b1 ?b2))
           )
           (and 
               (clear ?b2)
               (on-table ?b1)
               (not (on ?b1 ?b2))
           )
       )
```


## Output

Please check out the [Fast Downward Website](http://www.fast-downward.org/TranslatorOutputFormat) for information on the output format. Differences are described in the following.

The first difference can be found in the version section, where you can find either `3.FOND` or `3.POND`, depending on the translator used. The next difference can be found in the operator section, where we changed some lines to describe the `oneof` keyword. See the following example.


```
#!

begin_operator
pick-up b1 b2
3                  
0 0
5 0
6 1
2                   # there are two different outcomes
4                     # the first outcome affects four variables
0 0 0 1                 # value of variable 0 changes from 0 to 1
0 1 -1 0                # value of variable 1 changes to 0  
0 5 0 1                 # value of variable 5 changes from 0 to 1
0 6 1 0                 # value of variable 6 changes from 1 to 0
2                     # the second outcome affects two variables
0 1 -1 0                # value of variable 1 changes to 0
0 6 1 5                 # value of variable 6 changes from 1 to 5
0                    
end_operator
```
The last line before `end_operator` describes the cost of this operator and the four lines after `begin_operator` describe the preconditions of the operator. This domain does not use operator costs (unit-cost problem), so the final line is 0 for all operators.

# POND translator 
---

The POND translator is an extension of the FOND translator. The main differences are the description of the initial state and the description of the operators. 

## Input: Initial belief state

In contrast to FOND planning, the initial state is no longer necessarily a single world state, but rather a set of world states (i.e., a belief state). In the description of the initial belief state, all facts that are initially *true* and all facts that are initially *unknown* are listed. Facts that are neither true nor unknown are false in the initial belief state and because of the closed world assumption they are not listed in the initial state description.

If all facts are initially known, there is nothing more to do. But if there are unknown facts you may have to describe the constraints which apply to them. Therefore you can write general propositional formulas using `and`, `or` and `not`. Furthermore you can use `oneof`, which is an n-ary exclusive or.

### Examples for describing the initial belief state

Assume a blocksworld task with three blocks, where initially nothing is known about the blocks, but where the usual constraints hold (e.g., each block is either on the table, or on exactly one other block or in the hand). This is one possible description of the initial belief state.


```
#!pddl

(:init
(unknown (on-table b1))
(unknown (clear b1))
(unknown (on b1 b2))
(unknown (on b1 b3))
(unknown (on-table b2))
(unknown (clear b2))
(unknown (on b2 b1))
(unknown (on b2 b3))
(unknown (on-table b3))
(unknown (clear b3))
(unknown (on b3 b1))
(unknown (on b3 b2))
(oneof (on-table b1) (on b1 b2) (on b1 b3) )
(oneof (on-table b2) (on b2 b1) (on b2 b3) )
(oneof (on-table b3) (on b3 b1) (on b3 b2) )
(oneof (clear b1) (on b2 b1) (on b3 b1) )
(oneof (clear b2) (on b1 b2) (on b3 b2) )
(oneof (clear b3) (on b1 b3) (on b2 b3) )
(or (on-table b1) (on-table b2) (on-table b3) )
(or (clear b1) (clear b2) (clear b3) )
(or (not (on b1 b2)) (not (on b3 b2)))
(or (not (on b1 b3)) (not (on b2 b3)))
(or (not (on b2 b1)) (not (on b3 b1)))
(or (not (on b2 b3)) (not (on b1 b3)))
(or (not (on b3 b1)) (not (on b2 b1)))
(or (not (on b3 b2)) (not (on b1 b2)))

; No cyclic stacks of blocks
(or (not (on b2 b1)) (not (on b1 b2)))
(or (not (on b1 b2)) (not (on b2 b1)))
(or (not (not (or (not (on b2 b3)) (not (on b3 b1)) ))) (not (on b1 b2)))
(or (not (not (or (not (on b3 b2)) (not (on b2 b1)) ))) (not (on b1 b3)))
(or (not (not (or (not (on b3 b1)) (not (on b1 b2)) ))) (not (on b2 b3)))
(or (not (not (or (not (on b1 b3)) (not (on b3 b2)) ))) (not (on b2 b1)))
(or (not (not (or (not (on b2 b1)) (not (on b1 b3)) ))) (not (on b3 b2)))
(or (not (not (or (not (on b1 b2)) (not (on b2 b3)) ))) (not (on b3 b1)))
(or (not (on b3 b1)) (not (on b1 b3)))
(or (not (on b1 b3)) (not (on b3 b1)))
(or (not (on b3 b2)) (not (on b2 b3)))
(or (not (on b2 b3)) (not (on b3 b2)))
)
```

The following blocksworld example shows an initial belief state where every fact is known. This does not differ from describing an initial state of a classical or a FOND planning task.


```
#!pddl

(:init 
(emptyhand)
(on b1 b3)
(on b2 b1)
(on-table b3)
(on-table b4)
(on b5 b4)
(clear b2)
(clear b5)
)
```
## Input: Operators

In POND planning there are sensing operators to observe facts of belief states. A sensing operator is specified by an `:observe` keyword and a list of facts that could be observed by applying it. A sensing operator could also have preconditions.

### Example

```
#!pddl
(:action sense-clear-one-block
 :parameters (?b1 - block)
 :observe (clear ?b1)
)

(:action sense-clear-two-blocks
 :parameters (?b1 ?b2 - block)
 :observe (clear ?b1) (clear ?b2)
)
```



### Output

There are some differences to the FOND output format induced by the initial belief state description and the sensing operators. This is an example of an initial state description.


```
#!

begin_state
3                        # three initially true facts
3 1
7 1
11 1
6                        # six oneof
12 0 4 0 5 0               # var 12 is 0 xor var 4 is 0 xor var 5 is 0
13 0 6 0 8 0
14 0 9 0 10 0
0 0 6 0 9 0
1 0 4 0 10 0
2 0 5 0 8 0
20                       # 20 formulas
or((12 0)(13 0)(14 0))
or((0 0)(1 0)(2 0))
or((4 1)(10 1))
or((5 1)(8 1))
or((6 1)(9 1))
or((8 1)(5 1))
or((9 1)(6 1))
or((10 1)(4 1))
or((6 1)(4 1))
or((4 1)(6 1))
or(or((8 1)(9 1))(4 1))
or(or((10 1)(6 1))(5 1))
or(or((9 1)(4 1))(8 1))
or(or((5 1)(10 1))(6 1))
or(or((6 1)(5 1))(10 1))
or(or((4 1)(8 1))(9 1))
or((9 1)(5 1))
or((5 1)(9 1))
or((10 1)(8 1))
or((8 1)(10 1))
end_state
```

In the output format the information about observations is appended after the operator cost line.

```
#!
begin_operator
pick-up b1 b2
2
0 0
5 0
2
4
0 0 0 1
0 1 -1 0
0 5 0 1
0 6 1 0
2
0 1 -1 0
0 6 1 5
0                     # operator cost
0                     # no observations
end_operator

begin_operator
sense-clear b1
0
1                     # one outcome
0                       # empty effect
0                     # operator cost
1                     # one observation 
0 0                     # observe if variable 0 has value 0
end_operator

```