set title "para-loops:para-loops:thread-pool - 24*amd64, 1.7.0_09-b05"
set xlabel "Threads"
set ylabel "Throughput[cycles/s]"
set xtics 1
#set key out

#set term png
#set output "para-loops_thread-pool%throughput.png"
#set term postscript eps enhanced
#set output "para-loops_thread-pool%throughput.eps"

plot 'while_thread-pool.dat' using 1:4:($4*$3)/100 with errorlines title "while",\
'while-new_thread-pool.dat' using 1:4:($4*$3)/100 with errorlines title "while-new",\
'for_thread-pool.dat' using 1:4:($4*$3)/100 with errorlines title "for"
pause -1
