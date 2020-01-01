#!/bin/bash
queuename=$1
filterFile=$2
program=$3
config=$4

cat $filterFile | while read line
    do
        temp="${program} configurationmode=\"file\" configurationpath=\"$config\" mutationfilter=\"${line}\"";
        qsub -l mem_free=1.0G -q $queuename<<MARKER
${temp}
MARKER
    done

