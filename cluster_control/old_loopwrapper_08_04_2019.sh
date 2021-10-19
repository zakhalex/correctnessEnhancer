#!/bin/bash
filterFile1=$1
filterFile2=$2
program=$3
mode=$4
config1=$5
config2=$6
rangestart=$7
rangeend=$8
queues=('short' 'medium' 'long')

if [[ $mode == *"list"* ]]
then
    for ((i=$rangestart; i<=$rangeend; i++))
    do
        temp="${program} mode=\"${mode}\" configurationmode=\"file\" configurationpath=\"${config1}${i}${config2}\"";
        echo "qsub -l mem_free=1.0G -q ${queues[i%3]} ${temp}"
        qsub -l mem_free=1.0G -q ${queues[i%3]}<<MARKER
${temp}
MARKER
done
exit
elif [[ $mode == *"test"* ]]
then
    filtertype='testfilter'
else
    filtertype='mutationfilter'
fi

for ((i=$rangestart; i<=$rangeend; i++))
do     
    cat ${filterFile1}${i}${filterFile2} | while read line
    do
        temp="${program} mode=\"${mode}\" configurationmode=\"file\" configurationpath=\"${config1}${i}${config2}\" ${filtertype}=\"${line}\"";
        echo "qsub -l mem_free=1.0G -q ${queues[i%3]} ${temp}"
        qsub -l mem_free=1.0G -q ${queues[i%3]}<<MARKER
${temp}
MARKER
    
    done
done
