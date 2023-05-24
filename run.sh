#!/bin/bash

mvn clean

result_file="raw_result_before"

[ -f "$result_file" ] && rm $result_file
for rep in 1 2 3
do
    mvn test -U -Plgpl,examples,-clean-libs,-release -Dmaven.test.failure.ignore=true -DfailIfNoTests=false -Dtest=MvccStructuresOverheadTest &> test.log
    grep -F '####' test.log >> $result_file
done

