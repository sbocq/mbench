set title "para-loops:thread-pool - 24*amd64, 1.7.0_04-ea-b06"
set xlabel "Threads"
set ylabel "Speedup"
set xtics 1
#set key out

#set term png
#set output "para-loops_thread-pool%speedup.png"
#set term postscript eps enhanced
#set output "para-loops_thread-pool%speedup.eps"

plot 'while_thread-pool.dat' using 1:5:($5*$3)/100 with errorlines title "while:thread-pool",\
'while-new_thread-pool.dat' using 1:5:($5*$3)/100 with errorlines title "while-new:thread-pool",\
'for_thread-pool.dat' using 1:5:($5*$3)/100 with errorlines title "for:thread-pool",\
'ideal-speedup.dat' using 1:5:($5*$3)/100 with errorlines title "ideal-speedup"
pause -1
