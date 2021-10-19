#!/bin/bash
arrayparams=()
for i in "$@"
do
case $i in
    -arr*|--array*)
    arrayparams+=( ${i#*=} )
    shift # past argument=value
    ;;

    *)
          # unknown option
    echo "Test ${i}"
    ;;
esac
done

for var in "${arrayparams[@]}"
do
  echo "${var}"
  # do something on $var
done

echo $(awk "NR==$SGE_TASK_ID" $1)

