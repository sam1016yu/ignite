#!/bin/bash

cd $HOME/ignite
./mvnw clean


test_target=modules/core/src/test/java/org/apache/ignite/internal/processors/cache/RebalanceIteratorLargeEntriesOOMTest.java
result_file="raw_result_after"

[ -f "$result_file" ] && rm $result_file

for percent in 1 2 3 4 5 6 7 8 9 10
do
    for repeat in 1 2 3
    do
    sed -i "s/.*private static final long INTERVAL.*/    private static final long INTERVAL = MAX_REGION_SIZE * ${percent} \/ (100 * NUM_LOAD_THREADS * PAYLOAD_SIZE);/" $test_target 
    ./mvnw clean test -U -Plgpl,examples,-clean-libs,-release -Dmaven.test.failure.ignore=true -DfailIfNoTests=false -Dtest=RebalanceIteratorLargeEntriesOOMTest &> test.log
    echo "###" >> $result_file
    echo "Percent:$percent" >> $result_file
    echo "Repeat:$repeat" >> $result_file
    grep -F '!!!' test.log >> $result_file
    done
done

mv $result_file $HOME
rm test.log