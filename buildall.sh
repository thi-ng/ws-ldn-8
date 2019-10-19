#!/bin/bash

echo "Building Example 1"
cd day1/ex01/ && clj -A:prod &
echo "Building Example 2"
cd day1/ex02/ && clj -A:prod &
echo "Building Example 3"
cd day1/ex03/ && clj -A:prod &
echo "Building Example 4"
cd day2/ex04/ && clj -A:prod &
echo "Building Example 5"
cd day2/ex05/ && clj -A:prod &
echo "Building Exmaple 6"
cd day3/ex06/ && clj -A:prod &
echo "Building Example 7"
cd day3/ex07/ && ./compile.sh && clj -A:prod &

FAIL=0

for job in `jobs -p`
do
    echo $job
    wait $job || let "FAIL+=1"
done

echo $FAIL

if [ "$FAIL" == "0" ];
then
    echo "Finished!"
else
    echo "FAIL! ($FAIL)"
fi
