#$1 and $2 example: java\ -jar\ example_bots/RandomBot.jar
if [ $# -gt 2 ]
then
	echo "Use only two arguments. If arguments have spaces, use \."
	exit
fi
Files=$(ls maps)
rm -f out.txt
rm -f new_out.txt
rm -f dump.txt
touch out.txt
touch new_out.txt
touch dump.txt
for file in $Files
do
	echo $file
	java -jar tools/PlayGame.jar maps/$file 1000 1000 log.txt "$1" "$2" 2>> out.txt > dump.txt
done
cat out.txt | sed '/^T/ d' > new_out.txt
winners=$(cat new_out.txt)
count1=0
count2=0
for line in $winners
do
	if [ "$line" = "2" ]
	then
		count2=$((count2+1))
	elif [ "$line" = "1" ]
	then
		count1=$((count1+1))
	fi
done
echo "Player 1: "$count1
echo "Player 2: "$count2
rm out.txt
rm new_out.txt
rm dump.txt