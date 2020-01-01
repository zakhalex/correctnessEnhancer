#!/bin/bash
queuename=$1
location=$2
program=$3

for filename in $location; do
    [ -e "$filename" ] || continue
    cat $filename | while read line
    do
        temp="${program} mutationfilter=\"${line}\"";
        qsub -l mem_free=1.0G -q $queuename<<MARKER
${temp}
MARKER
    done
done
