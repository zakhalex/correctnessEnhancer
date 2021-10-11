# correctnessEnhancer

Main mode of operations:
"list" - preliminary analysis and environment preparation
"mutate" - mutant generation
"validate" - Test validation

Sample launch for SGE:

./test_loopwrapper.sh -p="java -jar /home/a/az68/mutation/correctnessEnhancer.jar" -m=list -c1="/home/a/az68/mutation/configs/Chart1/Chart_" -c2="b.config" -rs=1 -re=26
./test_loopwrapper.sh -tf1="/research/sp_temporary/az68/input/Chart_" -tf2="b_test_filter.txt" -mf1="/research/sp_temporary/az68/input/Chart_" -mf2="b_mutant_filter.txt" -p="java -jar /home/a/az68/mutation/correctnessEnhancer.jar" -m="test" =c1="/home/a/az68/mutation/configs/Chart1/Chart_" -c2="b.config" -rs=1 -re=26
