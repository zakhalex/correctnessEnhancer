echo $(awk "NR==$SGE_TASK_ID" $1)
