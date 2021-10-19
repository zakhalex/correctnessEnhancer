#!/bin/bash

program=$1
shift
echo "${program} dbcontrol=$SGE_TASK_ID" "$@"
eval "${program} dbcontrol=$SGE_TASK_ID" "$@"
