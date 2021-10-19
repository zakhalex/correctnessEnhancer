#!/bin/bash
for i in "$@"
do
case $i in
    -tf1=*|--testfilter1=*)
    testFilterFile1="${i#*=}"
    shift # past argument=value
    ;;
    -tf2=*|--testfilter2=*)
    testFilterFile2="${i#*=}"
    shift # past argument=value
    ;;

    -rs=*|--rangestart=*)
    rangestart="${i#*=}"
    shift # past argument=value
    ;;
    -re=*|--rangeend=*)
    rangeend="${i#*=}"
    shift # past argument=value
    ;;

    -db2=*|--dbconfig2=*)
    dbconfig2="${i#*=}"
    shift # past argument=value
    ;;
    -db1=*|--dbconfig1=*)
    dbconfig1="${i#*=}"
    shift # past argument=value
    ;;

    -mf1=*|--mutationfilter1=*)
    mutantFilterFile1="${i#*=}"
    shift # past argument=value
    ;;
    -mf2=*|--mutationfilter2=*)
    mutantFilterFile2="${i#*=}"
    shift # past argument=value
    ;;

    -db=*|--dbcontrol=*)
    dbcontrol="${i#*=}"
    shift # past argument=value
    ;;

    -l=*|--launcher=*)
    launcher="${i#*=}"
    shift # past argument=value
    ;;

    -p=*|--program=*)
    program="${i#*=}"
    shift # past argument=value
    ;;
    -m=*|--mode=*)
    mode="${i#*=}"
    shift # past argument=value
    ;;

    -c1=*|--config1=*)
    config1="${i#*=}"
    shift # past argument=value
    ;;
    -c2=*|--config2=*)
    config2="${i#*=}"
    shift # past argument=value
    ;;

    -o=*|--output=*)
    output="${i#*=}"
    shift # past argument=value
    ;;
    -e=*|--error=*)
    error="${i#*=}"
    shift # past argument=value
    ;;


    *)
          # unknown option
    ;;
esac
done

#filterFile1=$1
#filterFile2=$2
#program=$3
#mode=$4
#config1=$5
#config2=$6
#rangestart=$7
#rangeend=$8
queues=('medium')
queuelen=${#queues[@]}

if [[ $mode == *"list"* ]]
then
    for ((i=$rangestart; i<=$rangeend; i++))
    do
        temp="${program} mode=\"${mode}\" configurationmode=\"file\" configurationpath=\"${config1}${i}${config2}\"";
        echo "qsub -l mem_free=1.0G -q ${queues[i%$queuelen]} ${temp}"
        qsub -o "/dev/null" -e "/dev/null" -l mem_free=1.0G -q ${queues[i%$queuelen]}<<MARKER
${temp}
MARKER
done
exit
elif [[ $mode == *"test"* ]]
then
    if [ "${dbcontrol}" ]
    then
        for ((i=$rangestart; i<=$rangeend; i++))
        do
            #echo "Getting ready to cat ${dbconfig1}${i}${dbconfig2}"
            cat ${dbconfig1}${i}${dbconfig2} | while read line
            do
                temp="${launcher} \"${program}\" mode=\"${mode}\" configurationmode=\"file\" configurationpath=\"${config1}${i}${config2}\"";
                echo "qsub -t 1-${line} -l mem_free=1.0G -q ${queues[i%$queuelen]} ${temp}"
                
                qsub -t 1-${line} -o "/dev/null" -e "/dev/null" -l mem_free=1.0G -q ${queues[i%$queuelen]}<<MARKER
${temp}
MARKER
                
            done
        done
    elif [ -z "${mutantFilterFile1}" ]
    then
        for ((i=$rangestart; i<=$rangeend; i++))
        do
            numoflines=wc -l ${testFilterFile1}${i}${testFilterFile2} | awk '{print $1;}'
            echo ${numoflines}
            
            cat ${testFilterFile1}${i}${testFilterFile2} | while read line
            do
                temp="${program} mode=\"${mode}\" configurationmode=\"file\" configurationpath=\"${config1}${i}${config2}\" testfilter=\"${line}\"";
                echo "qsub -l mem_free=1.0G -q ${queues[i%$queuelen]} ${temp}"
                
                qsub -o "/dev/null" -e "/dev/null" -l mem_free=1.0G -q ${queues[i%$queuelen]}<<MARKER
${temp}
MARKER

            done
        done
    else
        for ((i=$rangestart; i<=$rangeend; i++))
        do
            numoflines=$(wc -l ${testFilterFile1}${i}${testFilterFile2} | awk '{print $1;}')
            echo ${numoflines}
            numoflines2=$(wc -l ${mutantFilterFile1}${i}${mutantFilterFile2} | awk '{print $1;}')
            echo ${numoflines2}
            echo $((${numoflines}*${numoflines2}))
            
            cat ${testFilterFile1}${i}${testFilterFile2} | while read line
            do
                cat ${mutantFilterFile1}${i}${mutantFilterFile2} | while read line2
                do
                    temp="-t 1-$((${numoflines}*${numoflines2}))"
                    if  [ -n "${output}" ]
                    then
                        temp+=" -o ${output}"
                    fi

                    if [ -n "${error}" ]
                    then
                        temp+=" -e ${error}"
                    fi
                    temp+=" ${program} mode=\"${mode}\" configurationmode=\"file\" configurationpath=\"${config1}${i}${config2}\" testfilter=\"${line}\" mutationfilter=\"${line2}\"";
                    echo "qsub -l mem_free=1.0G -q ${queues[i%$queuelen]} ${temp}"
                    
                    qsub -o "/dev/null" -e "/dev/null" -l mem_free=1.0G -q ${queues[i%$queuelen]} <<MARKER
${temp}
MARKER
                done
            done
        done

    fi
elif [[ $mode == *"mutate"* ]] 
then
    if [ "${dbcontrol}" ]
    then
        for ((i=$rangestart; i<=$rangeend; i++))
        do
            #echo "Getting ready to cat ${dbconfig1}${i}${dbconfig2}"
            cat ${dbconfig1}${i}${dbconfig2} | while read line
            do
                temp="${launcher} \"${program}\" mode=\"${mode}\" configurationmode=\"file\" configurationpath=\"${config1}${i}${config2}\"";
                echo "qsub -t 1-${line} -l mem_free=1.0G -q ${queues[i%$queuelen]} ${temp}"

                qsub -t 1-${line} -o "/dev/null" -e "/dev/null" -l mem_free=1.0G -q ${queues[i%$queuelen]}<<MARKER
${temp}
MARKER

            done
        done
    else
        for ((i=$rangestart; i<=$rangeend; i++))
        do
            cat ${mutantFilterFile1}${i}${mutantFilterFile2} | while read line
            do
                temp="${program} mode=\"${mode}\" configurationmode=\"file\" configurationpath=\"${config1}${i}${config2}\" mutationfilter=\"${line}\"";
                echo "qsub -l mem_free=1.0G -q ${queues[i%$queuelen]} ${temp}"
            
                qsub -l mem_free=1.0G -q ${queues[i%$queuelen]}<<MARKER
${temp}
MARKER

            done
        done
    fi
else
    echo "Mode not recognized"
fi
