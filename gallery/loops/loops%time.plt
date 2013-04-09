set title "loops - 24*amd64, 1.7.0_04-ea-b06"
set xlabel "Cycles"
set ylabel "Time[s]"
#set key out

#set term png
#set output "loops%time.png"
#set term postscript eps enhanced
#set output "loops%time.eps"

plot 'while.dat' using 1:2:($2*$3)/100 with errorlines title "while",\
'for.dat' using 1:2:($2*$3)/100 with errorlines title "for",\
'while-new.dat' using 1:2:($2*$3)/100 with errorlines title "while-new"
pause -1
